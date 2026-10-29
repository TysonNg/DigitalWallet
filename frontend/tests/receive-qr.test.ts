import test from "node:test";
import assert from "node:assert/strict";
import { proxyMediaContent } from "../src/server/media-content";
import { safeReturnTo } from "../src/i18n/paths";

const id = "a1234567-1234-4234-8234-123456789abc";

test("receive return URL survives login and locale changes with strict UUID paths", () => {
    assert.equal(safeReturnTo(`/vi/receive/${id}`, "en"), `/en/receive/${id}`);
    for (const path of [
        "https://evil.test",
        `/en/receive/${id}/extra`,
        "/en/receive/invalid",
        `/en/receive/%2e%2e`,
    ]) {
        assert.equal(safeReturnTo(path, "en"), "/en/dashboard");
    }
});

test("media proxy rejects path injection before authentication or network access", async () => {
    for (const value of [
        "../secret",
        "%2fsecret",
        "https://evil.test",
        `${id}?url=evil`,
        "invalid",
    ]) {
        const response = await proxyMediaContent(
            value,
            "https://backend.test/api/v1",
            async () => {
                throw Error("must not authenticate");
            },
            async () => {
                throw Error("must not fetch");
            }
        );
        assert.equal(response.status, 400);
        assert.equal(response.headers.get("cache-control"), "no-store");
    }
});

test("media proxy requires authentication and sanitizes refresh failures", async () => {
    const noFetch = async () => {
        throw Error("must not fetch");
    };
    assert.equal(
        (await proxyMediaContent(id, "https://backend.test", async () => null, noFetch)).status,
        401
    );
    const rejected = await proxyMediaContent(
        id,
        "https://backend.test",
        async () => {
            throw Object.assign(Error("secret JWT"), { statusCode: 401 });
        },
        noFetch
    );
    assert.equal(rejected.status, 401);
    assert.doesNotMatch(await rejected.text(), /secret/);
});

test("media proxy fetches fixed authenticated PNG path with no cache or redirects", async () => {
    const bytes = new Uint8Array([137, 80, 78, 71]);
    const response = await proxyMediaContent(
        id,
        "https://backend.test/api/v1",
        async () => "private-token",
        async (url, options) => {
            assert.equal(url, `https://backend.test/api/v1/media/${id}/content`);
            assert.equal(
                new Headers(options?.headers).get("Authorization"),
                "Bearer private-token"
            );
            assert.equal(options?.cache, "no-store");
            assert.equal(options?.redirect, "error");
            return new Response(bytes, {
                headers: {
                    "Content-Type": "image/png",
                    "Set-Cookie": "secret=1",
                    Authorization: "secret",
                },
            });
        }
    );
    assert.equal(response.status, 200);
    assert.deepEqual(new Uint8Array(await response.arrayBuffer()), bytes);
    assert.equal(response.headers.get("cache-control"), "no-store");
    assert.equal(response.headers.get("set-cookie"), null);
    assert.equal(response.headers.get("authorization"), null);
    assert.equal(response.headers.get("x-content-type-options"), "nosniff");
});

test("media proxy handles upstream auth, missing, invalid MIME and network errors without leaking bodies", async () => {
    for (const status of [401, 403, 404, 500]) {
        const response = await proxyMediaContent(
            id,
            "https://backend.test",
            async () => "token",
            async () => new Response("upstream-secret", { status })
        );
        assert.equal(response.status, status === 500 ? 502 : status);
        assert.doesNotMatch(await response.text(), /upstream-secret/);
    }
    const invalid = await proxyMediaContent(
        id,
        "https://backend.test",
        async () => "token",
        async () =>
            new Response("<script>bad</script>", { headers: { "Content-Type": "text/html" } })
    );
    assert.equal(invalid.status, 502);
    const failed = await proxyMediaContent(
        id,
        "https://backend.test",
        async () => "token",
        async () => {
            throw Error("network-secret");
        }
    );
    assert.equal(failed.status, 502);
    assert.doesNotMatch(await failed.text(), /network-secret/);
});
