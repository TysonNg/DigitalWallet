import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { JsxEmit, ModuleKind, transpileModule } from "typescript";
import * as jsxRuntime from "react/jsx-runtime";
import type { ReactElement } from "react";
import { isReceiveUuid } from "../src/features/wallet/receive-qr";

function receivePage() {
    const code = transpileModule(
        readFileSync("src/app/[locale]/receive/[token]/page.tsx", "utf8"),
        { compilerOptions: { module: ModuleKind.CommonJS, jsx: JsxEmit.ReactJSX } }
    ).outputText;
    const exports = {} as {
        default: (props: {
            params: Promise<{ locale: string; token: string }>;
        }) => Promise<ReactElement<{ token: string; loginHref: string }>>;
    };
    const require = (name: string) => {
        if (name === "react/jsx-runtime") return jsxRuntime;
        if (name === "next/navigation")
            return {
                notFound: () => {
                    throw Error("Not found");
                },
            };
        if (name === "@/features/wallet/receive-qr") return { isReceiveUuid };
        if (name === "@/features/wallet/components/receive-recipient-view")
            return { ReceiveRecipientView: () => null };
        throw Error(`Unexpected dependency: ${name}`);
    };
    new Function("require", "exports", code)(require, exports);

    return exports.default;
}

test("receive page gives each token a distinct React identity to reset prior recipient state", async () => {
    const page = receivePage();
    const tokens = ["a1234567-1234-4234-8234-123456789abc", "b1234567-1234-4234-8234-123456789abc"];
    const elements = await Promise.all(
        tokens.map((token) => page({ params: Promise.resolve({ locale: "en", token }) }))
    );

    for (const [index, element] of elements.entries()) {
        assert.equal(element.key, tokens[index]);
        assert.equal(element.props.token, tokens[index]);
        assert.equal(
            element.props.loginHref,
            `/?auth=login&returnTo=${encodeURIComponent(`/en/receive/${tokens[index]}`)}`
        );
    }
    assert.notEqual(elements[0].key, elements[1].key);
});
