import test from "node:test";
import assert from "node:assert/strict";
import { changePasswordAction } from "../src/features/auth/actions/auth.actions";

test("changePasswordAction rejects empty password inputs", async () => {
    const result = await changePasswordAction({
        currentPassword: "",
        newPassword: "password123",
    });
    assert.equal(result.success, false);
});

test("changePasswordAction rejects new password shorter than 6 characters", async () => {
    const result = await changePasswordAction({
        currentPassword: "oldpassword123",
        newPassword: "123",
    });
    assert.equal(result.success, false);
    assert.match(result.message, /6 ký tự/i);
});
