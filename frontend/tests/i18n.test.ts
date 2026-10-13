import test from "node:test";
import assert from "node:assert/strict";
import { NextRequest } from "next/server";
import { proxy } from "../src/proxy";
import { safeReturnTo } from "../src/i18n/paths";

function request(path: string, cookie?: string) {
    return new NextRequest("https://wallet.test" + path, {
        headers: {
            ...(cookie ? { cookie: "NEXT_LOCALE=" + cookie } : {}),
            "accept-language": "en-US",
        },
    });
}
test("URL wins cookie and synchronizes secure cookie", () => {
    const response = proxy(request("/en?auth=login", "vi"));
    assert.equal(response.headers.get("location"), null);
    assert.equal(response.cookies.get("NEXT_LOCALE")?.value, "en");
    assert.match(response.headers.get("set-cookie") || "", /Secure/i);
});
test("unprefixed URLs preserve query and use cookie", () => {
    assert.equal(
        proxy(request("/dashboard?filter=all", "en")).headers.get("location"),
        "https://wallet.test/en/dashboard?filter=all"
    );
});
test("missing or invalid cookie uses Vietnamese independent of browser", () => {
    for (const cookie of [undefined, "fr"])
        assert.equal(proxy(request("/", cookie)).headers.get("location"), "https://wallet.test/vi");
});
test("unsupported locales are rejected", () => {
    assert.equal(proxy(request("/fr/dashboard", "en")).status, 404);
});
test("API and assets bypass locale handling", () => {
    for (const path of [
        "/api/payments/webhook",
        "/_next/static/file.js",
        "/favicon.ico",
        "/sitemap.xml",
    ]) {
        const response = proxy(request(path));
        assert.equal(response.headers.get("location"), null);
        assert.equal(response.cookies.get("NEXT_LOCALE"), undefined);
    }
});
test("return destination accepts dashboard only and changes locale", () => {
    assert.equal(
        safeReturnTo("/vi/dashboard?filter=all#transactions", "en"),
        "/en/dashboard?filter=all#transactions"
    );
    for (const value of [
        "https://evil.test",
        "//evil.test",
        "/en/login",
        "/en?auth=login",
        "/dashboard/../login",
        "/%2f%2fevil.test",
        "/dashboard\\evil",
        "javascript:alert(1)",
    ]) {
        assert.equal(safeReturnTo(value, "en"), "/en/dashboard");
    }
});

test("popup history preserves unrelated query and registration state", async () => {
    const { useAuthStore } = await import("../src/features/auth/store/auth.store");
    let current = new URL("https://wallet.test/en?campaign=demo#features");
    const history: string[] = [];
    const previous = globalThis.window;
    Object.defineProperty(globalThis, "window", {
        configurable: true,
        value: {
            get location() {
                return current;
            },
            history: {
                pushState(_state: unknown, _title: string, href: string) {
                    history.push(href);
                    current = new URL(href, current);
                },
                replaceState(_state: unknown, _title: string, href: string) {
                    current = new URL(href, current);
                },
            },
        },
    });
    try {
        const store = useAuthStore.getState();
        store.openAuthModal("register");
        assert.equal(current.searchParams.get("auth"), "register");
        store.setPendingEmail("test@example.com");
        store.setRegistrationStep("verify");
        store.syncAuthModalFromUrl("register");
        assert.equal(useAuthStore.getState().registrationStep, "verify");
        assert.equal(useAuthStore.getState().pendingEmail, "test@example.com");
        store.syncAuthModalFromUrl(null);
        assert.equal(useAuthStore.getState().isAuthModalOpen, false);
        store.syncAuthModalFromUrl("register");
        assert.equal(useAuthStore.getState().registrationStep, "verify");
        const writes = history.length;
        store.syncAuthModalFromUrl("login");
        assert.equal(history.length, writes);
        current.searchParams.set("returnTo", "/en/dashboard");
        store.closeAuthModal();
        assert.equal(current.searchParams.get("auth"), null);
        assert.equal(current.searchParams.get("returnTo"), null);
        assert.equal(current.searchParams.get("campaign"), "demo");
        assert.equal(current.hash, "#features");
    } finally {
        Object.defineProperty(globalThis, "window", { configurable: true, value: previous });
        useAuthStore.getState().resetRegistration();
    }
});

test("auth validation uses selected language without changing constraints", async () => {
    const { createLoginSchema, createVerifyOtpSchema } =
        await import("../src/features/auth/schemas/auth.schema");
    for (const locale of ["vi", "en"]) {
        const messages = (await import("../messages/" + locale + "/authValidation.json")).default
            .authValidation;
        const translator = (key: keyof typeof messages) => messages[key];
        const result = createLoginSchema(translator).safeParse({
            email: "invalid",
            password: "short",
        });
        assert.equal(result.success, false);
        if (!result.success) assert.equal(result.error.issues[0].message, messages.email);
        assert.equal(
            createLoginSchema(translator).safeParse({
                email: "test@example.com",
                password: "123456",
            }).success,
            true
        );
        assert.equal(
            createVerifyOtpSchema(translator).safeParse({
                email: "test@example.com",
                otp: "123456",
            }).success,
            true
        );
        assert.equal(
            createVerifyOtpSchema(translator).safeParse({
                email: "test@example.com",
                otp: "abcdef",
            }).success,
            false
        );
    }
});

test("Vietnamese metadata and language selector retain Unicode", async () => {
    const { readFile } = await import("node:fs/promises");
    const metadata = await readFile("src/app/[locale]/page.tsx", "utf8");
    const selector = await readFile("src/components/language-switcher.tsx", "utf8");
    assert.ok(metadata.includes("Ví điện tử"));
    assert.ok(selector.includes("Tiếng Việt"));
});

test("server-action POST with the selected locale does not rewrite its cookie", () => {
    const response = proxy(
        new NextRequest("https://wallet.test/vi/dashboard", {
            method: "POST",
            headers: { cookie: "NEXT_LOCALE=vi", "Next-Action": "action-id" },
        })
    );
    assert.equal(response.headers.get("set-cookie"), null);
    assert.equal(response.headers.get("location"), null);
});
