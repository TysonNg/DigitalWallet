import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { ModuleKind, transpileModule } from "typescript";
import { validateWithdrawal } from "../src/features/wallet/stripe-withdrawal";
import type { WithdrawalRequest } from "../src/features/wallet/types/stripe-withdrawal.types";

class BackendError extends Error {
    errorCode = "WITHDRAWAL_REJECTED";
}

// Run the real server actions with only their authenticated network boundary replaced.
function actions(backendFetch: (path: string, options?: RequestInit) => Promise<unknown>) {
    const source = readFileSync("src/features/wallet/actions/stripe-withdrawal.actions.ts", "utf8");
    const code = transpileModule(source, {
        compilerOptions: { module: ModuleKind.CommonJS },
    }).outputText;
    const exports = {} as typeof import("../src/features/wallet/actions/stripe-withdrawal.actions");
    const require = (name: string) => {
        if (name === "@/server/backend-client") return { backendFetch, BackendError };
        if (name === "../stripe-withdrawal") return { validateWithdrawal };
        throw new Error(`Unexpected dependency: ${name}`);
    };
    new Function("require", "exports", code)(require, exports);

    return exports;
}

const request: WithdrawalRequest = {
    walletId: "wallet-1",
    amount: 25250,
    idempotencyKey: "f1234567-89ab-4cde-8123-456789abcdef",
    testBank: "FAILURE",
    pin: "012345",
};

test("server action posts the exact sandbox request and preserves its UUID on retry", async () => {
    const bodies: unknown[] = [];
    const api = actions(async (path, options) => {
        assert.equal(path, "/wallet/withdrawals");
        assert.equal(options?.method, "POST");
        assert.equal(options?.cache, "no-store");
        bodies.push(JSON.parse(String(options?.body)));
        return {
            success: true,
            data: { id: "withdrawal-1", mode: "STRIPE_TEST", status: "PENDING" },
        };
    });
    const result = await api.createStripeWithdrawalAction(request);
    assert.equal(result.success, true);
    if (result.success) assert.equal(result.data.status, "PENDING");
    await api.createStripeWithdrawalAction(request);
    assert.deepEqual(bodies, [request, request]);
});

test("backend rejection, transport errors and malformed responses never report success", async () => {
    for (const backend of [
        async () => {
            throw new BackendError("PIN rejected");
        },
        async () => {
            throw new TypeError("Network down");
        },
        async () => ({ success: false, data: null }),
        async () => ({ success: true, data: null }),
        async () => ({ success: true, data: { id: "live", mode: "LIVE" } }),
    ]) {
        const result = await actions(backend).createStripeWithdrawalAction(request);
        assert.equal(result.success, false);
        if (!result.success) assert.ok(result.message);
    }
    const rejection = await actions(async () => {
        throw new BackendError("PIN rejected");
    }).createStripeWithdrawalAction(request);
    assert.deepEqual(rejection, {
        success: false,
        message: "PIN rejected",
        errorCode: "WITHDRAWAL_REJECTED",
    });
});

test("invalid request is rejected before reaching the backend", async () => {
    let calls = 0;
    const api = actions(async () => {
        calls++;
        return {};
    });
    for (const input of [
        { ...request, amount: 25100 },
        { ...request, idempotencyKey: "not-a-uuid" },
        { ...request, pin: "123" },
    ]) {
        assert.equal((await api.createStripeWithdrawalAction(input)).success, false);
    }
    assert.equal(calls, 0);
});

test("history uses the encoded wallet query without caching and propagates load failures", async () => {
    const api = actions(async (path, options) => {
        assert.equal(path, "/wallet/withdrawals?walletId=wallet%20%26%20other");
        assert.equal(options?.cache, "no-store");
        return { success: true, data: [] };
    });
    assert.deepEqual(await api.getStripeWithdrawalsAction("wallet & other"), {
        success: true,
        data: [],
    });
    const failed = actions(async () => {
        throw new TypeError("Network down");
    });
    assert.equal((await failed.getStripeWithdrawalsAction("wallet")).success, false);
});
