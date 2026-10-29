import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { ModuleKind, transpileModule } from "typescript";
import { isReceiveUuid } from "../src/features/wallet/receive-qr";

const token = "a1234567-1234-4234-8234-123456789abc";

class BackendError extends Error {
    constructor(
        message: string,
        public statusCode: number,
        public errorCode?: string
    ) {
        super(message);
    }
}

function actions(
    backendFetch: (path: string, options?: RequestInit) => Promise<unknown>,
    loggedIn = true
) {
    const code = transpileModule(
        readFileSync("src/features/wallet/actions/receive-qr.actions.ts", "utf8"),
        {
            compilerOptions: { module: ModuleKind.CommonJS },
        }
    ).outputText;
    const exports = {} as typeof import("../src/features/wallet/actions/receive-qr.actions");
    const require = (name: string) => {
        if (name === "@/server/backend-client") return { backendFetch, BackendError };
        if (name === "@/server/session")
            return {
                getSession: async () => ({
                    isLoggedIn: loggedIn,
                    user: loggedIn ? { id: "owner" } : undefined,
                }),
            };
        if (name === "next-intl/server")
            return { getTranslations: async () => (key: string) => key };
        if (name === "../receive-qr") return { isReceiveUuid };
        throw Error(`Unexpected dependency: ${name}`);
    };
    new Function("require", "exports", code)(require, exports);

    return exports;
}

test("receive actions require session and reject invalid tokens before calling backend", async () => {
    const api = actions(async () => {
        assert.fail("must not call backend");
    }, false);
    assert.equal((await api.getReceiveQrAction()).errorCode, "SESSION_EXPIRED");
    assert.equal((await api.resolveReceiveRecipientAction(token)).errorCode, "SESSION_EXPIRED");
    const authenticated = actions(async () => {
        assert.fail("must not call backend");
    });
    for (const input of ["invalid", "../lookup", `${token}/recipient`, `${token}?walletId=evil`]) {
        assert.equal(
            (await authenticated.resolveReceiveRecipientAction(input)).errorCode,
            "INVALID_RECEIVE_TOKEN"
        );
    }
});

test("receive actions use authenticated backend contracts without caching or client supplied destinations", async () => {
    const qr = {
        mediaId: token,
        contentUrl: `/media/${token}/content`,
        receiveUrl: `https://wallet.test/vi/receive/${token}`,
    };
    const recipient = {
        walletId: "current-wallet",
        fullName: "Current Recipient",
        currency: "VND",
    };
    const requests: string[] = [];
    const api = actions(async (path, options) => {
        requests.push(path);
        assert.equal(options?.cache, "no-store");
        assert.equal(options?.body, undefined);
        if (path === "/wallet/receive-qr") {
            assert.equal(options?.method, "POST");
            return { success: true, data: qr };
        }
        assert.equal(path, `/wallet/receive-qr/${token}/recipient`);
        assert.equal(options?.method, "GET");
        return { success: true, data: recipient };
    });
    assert.deepEqual((await api.getReceiveQrAction()).data, qr);
    assert.deepEqual((await api.resolveReceiveRecipientAction(token)).data, recipient);
    assert.equal(requests.length, 2);
});

test("receive actions preserve recipient rejection and identify expired sessions without leaking transport errors", async () => {
    for (const errorCode of [
        "CANNOT_TRANSFER_TO_SELF",
        "WALLET_NOT_ACTIVE",
        "RECEIVE_CODE_NOT_FOUND",
    ]) {
        const api = actions(async () => {
            throw new BackendError("Recipient rejected", 400, errorCode);
        });
        const result = await api.resolveReceiveRecipientAction(token);
        assert.equal(result.success, false);
        assert.equal(result.errorCode, errorCode);
        assert.equal(result.data, undefined);
    }
    const expired = actions(async () => {
        throw new BackendError("Expired", 401);
    });
    assert.equal((await expired.resolveReceiveRecipientAction(token)).errorCode, "SESSION_EXPIRED");
    const unavailable = actions(async () => {
        throw Error("private network URL or token");
    });
    assert.equal((await unavailable.getReceiveQrAction()).message, "failed");
});
