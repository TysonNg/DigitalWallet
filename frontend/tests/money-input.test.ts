import test from "node:test";
import assert from "node:assert/strict";

test("money input groups long amounts and parses formatted paste without losing precision", async () => {
    const { formatMoneyInput, parseMoneyInput } = await import("../src/lib/money-input");
    assert.equal(formatMoneyInput(1234567890, "vi"), "1.234.567.890");
    assert.equal(formatMoneyInput(1234567890, "en"), "1,234,567,890");
    assert.equal(formatMoneyInput(0, "vi"), "");
    assert.equal(parseMoneyInput("1.234.567.890"), 1234567890);
    assert.equal(parseMoneyInput("1,234,567,890"), 1234567890);
    assert.equal(parseMoneyInput(""), 0);
    for (const value of ["9007199254740992", "-1000", "1e6", "abc", "1.5", "1,5", "..", "12.34"]) {
        assert.equal(parseMoneyInput(value), null);
    }
});
