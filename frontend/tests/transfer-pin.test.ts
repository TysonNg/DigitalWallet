import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import { resolve } from "node:path";
import { isValidElement, type ReactElement } from "react";
import { transpileModule, ModuleKind, JsxEmit } from "typescript";
import { initializeTransferForm } from "../src/features/wallet/transfer-form";

function transferModal(
    mutate: (data: Record<string, unknown>) => Promise<void> = async () => {},
    draft = true
) {
    const values: unknown[] = [];
    const effects: (() => void)[] = [];
    const transfers: Record<string, unknown>[] = [];
    const successes: unknown[] = [];
    let cursor = 0;
    let closed = 0;
    let completed = 0;
    let dirty = false;
    const PinModal = () => null;
    const path = resolve("src/features/wallet/components/transfer-modal.tsx");
    const require = createRequire(path);
    const exports: Record<string, (props: object) => ReactElement> = {};
    const props = {
        isOpen: true, onClose() { closed++; }, onSuccess() { completed++; },
        senderWalletId: "sender", currentBalance: 1000000,
        initialRecipient: { identifier: "0901234567", walletId: "recipient", fullName: "Nam" },
        ...(draft ? { initialAmount: 50000, initialDescription: "  ăn trưa  " } : {}),
    };
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
            useRef(initial: unknown) {
                const index = cursor++;
                return values[index] ??= { current: initial };
            },
            useEffect(effect: () => void, deps: unknown[]) {
                const index = cursor++;
                const previous = values[index] as unknown[] | undefined;
                if (!previous || deps.some((value, i) => !Object.is(value, previous[i]))) effects.push(effect);
                values[index] = deps;
            },
        };
        if (name === "next-intl") return { useTranslations: () => (key: string) => key, useLocale: () => "vi" };
        if (name === "../transfer-form") return { initializeTransferForm };
        if (name === "./pin-modal") return { PinModal };
        if (name === "../hooks/use-wallet") return {
            ActionError: class extends Error {},
            useBeneficiariesQuery: () => ({ data: [] }),
            useAddBeneficiaryMutation: () => ({}),
            useTransferMutation: () => ({ isPending: false, mutateAsync: async (data: Record<string, unknown>) => {
                transfers.push(data);
                await mutate(data);
            } }),
        };
        if (name === "../actions/wallet.actions") return {};
        if (name === "sonner") return { toast: { success: (value: unknown) => successes.push(value), error() {} } };
        if (name.startsWith("@/components/")) return new Proxy({}, { get: (_, key) => String(key) });
        return require(name);
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
            tree = exports.TransferModal(props);
            effects.splice(0).forEach((effect) => effect());
        } while (dirty);
        return elements(tree);
    };
    const pin = () => render().find((element) => element.type === PinModal)?.props;
    const submit = async () => {
        const form = render().find((element) => element.type === "form");
        assert.ok(form);
        await (form.props.onSubmit as (event: object) => Promise<void>)({ preventDefault() {} });
    };
    return {
        transfers, successes, pin, submit,
        completed: () => completed, closed: () => closed,
        confirm: (value: string) => {
            const modal = pin();
            assert.ok(modal, "PIN confirmation must be open");
            return (modal.onConfirm as (pin: string) => Promise<void>)(value);
        },
        close() { props.isOpen = false; render(); },
        reopen() { props.isOpen = true; render(); },
    };
}

test("draft and manual transfer submission require PIN before mutation", async () => {
    for (const draft of [true, false]) {
        const ui = transferModal(undefined, draft);
        await ui.submit();
        assert.equal(ui.transfers.length, 0);
        assert.ok(ui.pin());
        await ui.confirm("123456");
        assert.deepEqual(ui.transfers, [{
            senderWalletId: "sender", receiverWalletId: "recipient",
            amount: draft ? 50000 : 200000,
            description: draft ? "  ăn trưa  " : "defaultTransferDescription", pin: "123456",
        }]);
        assert.equal(ui.successes.length, 1);
        assert.equal(ui.completed(), 1);
        assert.equal(ui.closed(), 1);
        assert.equal(ui.pin(), undefined);
    }
});

test("invalid PIN and cancellation never transfer; stale confirmation cannot transfer", async () => {
    const ui = transferModal();
    await ui.submit();
    for (const value of ["", "12345", "1234567", "abcdef", " 123456"]) {
        await assert.rejects(ui.confirm(value));
    }
    const modal = ui.pin()!;
    (modal.onClose as () => void)();
    assert.equal(ui.pin(), undefined);
    await (modal.onConfirm as (pin: string) => Promise<void>)("123456");
    assert.equal(ui.transfers.length, 0);
    assert.equal(ui.successes.length, 0);
});

test("failed PIN keeps confirmation open for retry and never reports success", async () => {
    let attempts = 0;
    const ui = transferModal(async () => {
        if (++attempts === 1) throw new Error("Incorrect PIN");
    });
    await ui.submit();
    await assert.rejects(ui.confirm("111111"), /Incorrect PIN/);
    assert.ok(ui.pin());
    assert.equal(ui.successes.length, 0);
    assert.equal(ui.completed(), 0);
    assert.equal(ui.closed(), 0);
    await ui.confirm("123456");
    assert.equal(ui.transfers.length, 2);
    assert.equal(ui.successes.length, 1);
    assert.equal(ui.pin(), undefined);
});

test("closing and reopening clears PIN confirmation and invalidates its callback", async () => {
    const ui = transferModal();
    await ui.submit();
    const modal = ui.pin()!;
    ui.close();
    ui.reopen();
    assert.equal(ui.pin(), undefined);
    await (modal.onConfirm as (pin: string) => Promise<void>)("123456");
    assert.equal(ui.transfers.length, 0);
    await ui.submit();
    assert.ok(ui.pin());
});
