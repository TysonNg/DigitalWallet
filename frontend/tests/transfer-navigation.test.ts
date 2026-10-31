import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import { resolve } from "node:path";
import { isValidElement, type ReactElement } from "react";
import { transpileModule, ModuleKind, JsxEmit } from "typescript";

// Run the real view with deterministic hook state and a controlled router/API boundary.
function transferView() {
    const values: unknown[] = [];
    const effects: (() => void)[] = [];
    let cursor = 0;
    let dirty = false;
    let query = new URLSearchParams("recipient=0901234567&amount=100000");
    const path = resolve("src/features/wallet/views/transfer-view.tsx");
    const realRequire = createRequire(path);
    const exports: Record<string, () => ReactElement> = {};
    const translate = Object.assign((key: string) => key, { has: () => false });
    const code = transpileModule(readFileSync(path, "utf8"), {
        compilerOptions: { module: ModuleKind.CommonJS, jsx: JsxEmit.ReactJSX },
    }).outputText;
    new Function("require", "exports", code)((name: string) => {
        if (name === "react") return {
            useState(initial: unknown) {
                const index = cursor++;
                if (!(index in values)) values[index] = typeof initial === "function" ? initial() : initial;
                return [values[index], (next: unknown) => {
                    const value = typeof next === "function" ? next(values[index]) : next;
                    dirty ||= !Object.is(values[index], value);
                    values[index] = value;
                }];
            },
            useRef(initial: unknown) { return values[cursor++] ??= { current: initial }; },
            useEffect(effect: () => (() => void) | void, deps: unknown[]) {
                const index = cursor++;
                const previous = values[index] as { deps: unknown[]; cleanup?: () => void } | undefined;
                if (!previous || deps.some((value, i) => !Object.is(value, previous.deps[i]))) {
                    previous?.cleanup?.();
                    const state = { deps, cleanup: undefined as (() => void) | undefined };
                    values[index] = state;
                    effects.push(() => { state.cleanup = effect() || undefined; });
                }
            },
        };
        if (name === "next/navigation") return { useSearchParams: () => query };
        if (name === "next-intl") return { useTranslations: () => translate, useLocale: () => "vi" };
        if (name === "../hooks/use-wallet") return {
            ActionError: class extends Error {},
            useMyWalletQuery: () => ({ data: { id: "sender", balance: 1000000, currency: "VND", pinSet: true } }),
            useBeneficiariesQuery: () => ({ data: [] }),
            useTransferMutation: () => ({ isPending: false }),
            useAddBeneficiaryMutation: () => ({}),
        };
        if (name === "../actions/wallet.actions") return {
            lookupRecipientAction: async () => ({ success: true, data: { walletId: "recipient", userId: "user", fullName: "Nam", currency: "VND" } }),
        };
        if (name === "../components/pin-modal") return { PinModal: "PinModal" };
        if (name === "@/i18n/navigation") return { Link: "Link" };
        if (name === "sonner") return { toast: { success() {}, error() {} } };
        if (name.startsWith("@/components/")) return new Proxy({}, { get: (_, key) => String(key) });
        return realRequire(name);
    }, exports);
    const elements = (node: unknown): ReactElement<Record<string, unknown>>[] => {
        if (Array.isArray(node)) return node.flatMap(elements);
        if (!isValidElement(node)) return [];
        const element = node as ReactElement<Record<string, unknown>>;
        return [element, ...elements(element.props.children)];
    };
    const render = () => {
        let tree: ReactElement;
        do {
            dirty = false;
            cursor = 0;
            tree = exports.TransferView();
            effects.splice(0).forEach((effect) => effect());
        } while (dirty);
        return elements(tree);
    };
    return { render, navigate(amount: number) { query = new URLSearchParams(`recipient=0901234567&amount=${amount}`); } };
}

test("amount-only URL navigation preserves a verified transfer recipient", async () => {
    const ui = transferView();
    ui.render();
    await new Promise((resolve) => setImmediate(resolve));
    let nodes = ui.render();
    assert.equal(nodes.find((node) => node.type === "Button" && node.props.type === "submit")?.props.disabled, false);
    ui.navigate(200000);
    nodes = ui.render();
    assert.equal(nodes.find((node) => node.props.id === "transfer-amount-input")?.props.value, 200000);
    assert.equal(nodes.find((node) => node.type === "Button" && node.props.type === "submit")?.props.disabled, false);
});
