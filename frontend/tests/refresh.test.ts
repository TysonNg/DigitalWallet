import test from "node:test";
import assert from "node:assert/strict";
import {
    createRefreshCoordinator,
    RefreshRejectedError,
    refreshSession,
} from "../src/server/refresh-coordinator";

test("concurrent refreshes reuse the rotated pair, including a late request", async () => {
    let calls = 0;
    const refresh = createRefreshCoordinator(async () => {
        calls++;
        await new Promise((resolve) => setTimeout(resolve, 10));
        return { accessToken: "new-access", refreshToken: "new-refresh" };
    });
    const results = await Promise.all([refresh("old"), refresh("old"), refresh("old")]);
    assert.deepEqual(results[0], { accessToken: "new-access", refreshToken: "new-refresh" });
    assert.equal(calls, 1);
    assert.deepEqual(await refresh("old"), results[0]);
    assert.equal(calls, 1);
});

test("temporary failures allow retry rather than poisoning the session", async () => {
    let calls = 0;
    const refresh = createRefreshCoordinator(async () => {
        if (++calls === 1) throw new Error("backend unavailable");
        return { accessToken: "recovered", refreshToken: "rotated" };
    });
    await assert.rejects(refresh("old"), /backend unavailable/);
    assert.equal((await refresh("old")).accessToken, "recovered");
});

test("rejected refresh tokens remain distinguishable from temporary errors", async () => {
    const refresh = createRefreshCoordinator(async () => {
        throw new RefreshRejectedError();
    });
    await assert.rejects(refresh("revoked"), RefreshRejectedError);
});

test("successful refresh results expire", async () => {
    let now = 0;
    let calls = 0;
    const refresh = createRefreshCoordinator(
        async () => ({ accessToken: String(++calls), refreshToken: "rotated" }),
        () => now
    );
    assert.equal((await refresh("old")).accessToken, "1");
    now = 30_001;
    assert.equal((await refresh("old")).accessToken, "2");
});

test("rotated tokens are persisted before returning the access token", async () => {
    let saved: unknown;
    const session = {
        accessToken: "expired",
        refreshToken: "old",
        async save() {
            saved = { accessToken: this.accessToken, refreshToken: this.refreshToken };
        },
        destroy() {
            throw new Error("valid session must survive");
        },
    };
    const token = await refreshSession(session, async () => ({
        accessToken: "new",
        refreshToken: "rotated",
    }));
    assert.equal(token, "new");
    assert.deepEqual(saved, { accessToken: "new", refreshToken: "rotated" });
});

test("temporary backend failures preserve the session", async () => {
    let destroyed = false;
    const session = {
        accessToken: "expired",
        refreshToken: "old",
        async save() {
            throw new Error("must not save after a failed exchange");
        },
        destroy() {
            destroyed = true;
        },
    };
    await assert.rejects(
        refreshSession(session, async () => {
            throw new Error("unavailable");
        }),
        /unavailable/
    );
    assert.equal(destroyed, false);
    assert.equal(session.refreshToken, "old");
});

test("definitively rejected tokens clear the session", async () => {
    let destroyed = false;
    const session = {
        refreshToken: "revoked",
        async save() {
            throw new Error("must not save a rejected session");
        },
        destroy() {
            destroyed = true;
        },
    };
    await assert.rejects(
        refreshSession(session, async () => {
            throw new RefreshRejectedError();
        }),
        RefreshRejectedError
    );
    assert.equal(destroyed, true);
});
