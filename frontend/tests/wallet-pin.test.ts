import test from "node:test";
import assert from "node:assert/strict";
import { setupWalletPinAction, changeWalletPinAction } from "../src/features/wallet/actions/wallet.actions";
import viApiErrors from "../messages/vi/apiErrors.json";
import enApiErrors from "../messages/en/apiErrors.json";

test("setupWalletPinAction rejects PIN not exactly 6 digits", async () => {
    const invalidShort = await setupWalletPinAction("12345");
    assert.equal(invalidShort.success, false);
    assert.equal(invalidShort.errorCode, "VALIDATION_ERROR");

    const invalidAlpha = await setupWalletPinAction("12345a");
    assert.equal(invalidAlpha.success, false);
    assert.equal(invalidAlpha.errorCode, "VALIDATION_ERROR");

    const invalidLong = await setupWalletPinAction("1234567");
    assert.equal(invalidLong.success, false);
    assert.equal(invalidLong.errorCode, "VALIDATION_ERROR");
});

test("changeWalletPinAction rejects identical old and new PIN", async () => {
    const result = await changeWalletPinAction("123456", "123456");
    assert.equal(result.success, false);
    assert.equal(result.errorCode, "NEW_PASSWORD_SAME_AS_OLD");
});

test("changeWalletPinAction rejects non 6-digit old or new PIN", async () => {
    const res1 = await changeWalletPinAction("123", "654321");
    assert.equal(res1.success, false);
    assert.equal(res1.errorCode, "VALIDATION_ERROR");

    const res2 = await changeWalletPinAction("123456", "65432");
    assert.equal(res2.success, false);
    assert.equal(res2.errorCode, "VALIDATION_ERROR");
});

test("PIN error codes exist in both Vietnamese and English dictionaries", () => {
    const pinErrorKeys = [
        "WALLET_PIN_NOT_SET",
        "INVALID_WALLET_PIN",
        "WALLET_PIN_LOCKED",
        "WEAK_WALLET_PIN",
        "WALLET_ALREADY_HAS_PIN",
    ] as const;

    for (const key of pinErrorKeys) {
        assert.ok((viApiErrors.apiErrors as Record<string, string>)[key], `Missing vi translation for ${key}`);
        assert.ok((enApiErrors.apiErrors as Record<string, string>)[key], `Missing en translation for ${key}`);
    }
});
