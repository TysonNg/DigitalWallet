import test from "node:test";
import assert from "node:assert/strict";
import {
    validateWithdrawal,
    createWithdrawalIntent,
} from "../src/features/wallet/stripe-withdrawal";

test("withdrawals enforce integer VND, boundaries, and 250 VND increments", () => {
    for (const amount of [NaN, Infinity, 24999, 25000.5, 25100, 100000250]) {
        assert.ok(validateWithdrawal({ walletId: "wallet", amount, testBank: "SUCCESS" }));
    }
    for (const amount of [25000, 25250, 100000000]) {
        assert.equal(validateWithdrawal({ walletId: "wallet", amount, testBank: "SUCCESS" }), null);
    }
});

test("withdrawals reject unknown banks, missing wallets, insufficient balance and invalid PIN", () => {
    const input = { walletId: "wallet", amount: 25000, testBank: "SUCCESS" as const };
    assert.ok(validateWithdrawal({ ...input, walletId: "" }));
    assert.ok(validateWithdrawal({ ...input, testBank: "REAL_BANK" as "SUCCESS" }));
    assert.ok(validateWithdrawal(input, { balance: 24999 }));
    assert.ok(validateWithdrawal(input, { hasPin: true }));
    assert.ok(validateWithdrawal({ ...input, pin: "12345a" }, { hasPin: true }));
    assert.equal(validateWithdrawal({ ...input, pin: "012345" }, { hasPin: true }), null);
});

test("an intent keeps its UUID for each wallet, amount and bank until confirmed", () => {
    let sequence = 0;
    const intent = createWithdrawalIntent(() => `uuid-${++sequence}`);
    const input = { walletId: "wallet", amount: 25000, testBank: "SUCCESS" as const };
    assert.equal(intent.keyFor(input), "uuid-1");
    assert.equal(intent.keyFor({ ...input }), "uuid-1");
    assert.equal(intent.keyFor({ ...input, amount: 25250 }), "uuid-2");
    assert.equal(intent.keyFor(input), "uuid-1");
    assert.equal(intent.keyFor({ ...input, testBank: "FAILURE" }), "uuid-3");
    assert.equal(intent.keyFor({ ...input, pin: "123456" }), "uuid-1");
    intent.confirm(input);
    assert.equal(intent.keyFor(input), "uuid-4");
    assert.equal(intent.keyFor({ ...input, walletId: "other" }), "uuid-5");
});

test("correcting PIN after an unknown response preserves the original request UUID", () => {
    let generated = 0;
    const intent = createWithdrawalIntent(() => `key-${++generated}`);
    const input = { walletId: "wallet", amount: 25000, testBank: "SUCCESS" as const, pin: "123456" };
    const original = intent.keyFor(input);
    assert.equal(intent.keyFor({ ...input, pin: "654321" }), original);
    assert.equal(intent.keyFor({ ...input, pin: undefined }), original);
    assert.equal(generated, 1);
});

test("session storage restores unresolved intents after reload without storing PIN", () => {
    const entries = new Map<string, string>();
    const storage = {
        getItem: (key: string) => entries.get(key) ?? null,
        setItem: (key: string, value: string) => { entries.set(key, value); },
        removeItem: (key: string) => { entries.delete(key); },
    };
    const input = { walletId: "wallet", amount: 25250, testBank: "FAILURE" as const, pin: "012345" };
    const first = createWithdrawalIntent(() => "original-key", storage);
    assert.equal(first.hasPending(input), false);
    assert.equal(first.keyFor(input), "original-key");
    assert.doesNotMatch(JSON.stringify([...entries]), /012345|pin/i);
    const restored = createWithdrawalIntent(() => "new-key", storage);
    assert.equal(restored.hasPending(input), true);
    assert.equal(restored.keyFor({ ...input, pin: "654321" }), "original-key");
    assert.equal(restored.keyFor({ ...input, amount: 25500 }), "new-key");
    assert.equal(restored.keyFor(input), "original-key");
    restored.confirm(input);
    const afterConfirmation = createWithdrawalIntent(() => "confirmed-new-key", storage);
    assert.equal(afterConfirmation.hasPending(input), false);
    assert.equal(afterConfirmation.keyFor(input), "confirmed-new-key");
    assert.equal(afterConfirmation.keyFor({ ...input, amount: 25500 }), "new-key");
});

test("default intent keys are UUIDs generated on the client", () => {
    const intent = createWithdrawalIntent();
    assert.match(
        intent.keyFor({ walletId: "wallet", amount: 25000, testBank: "SUCCESS" }),
        /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
    );
});
