import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import { resolve } from "node:path";
import { transpileModule, ModuleKind, JsxEmit } from "typescript";
import { isValidElement, type ReactElement } from "react";

// Exercise component handlers with deterministic hook state; keep real React JSX elements.
function drawer() {
    const values: unknown[] = [];
    let cursor = 0;
    let finish!: () => void;
    let fail!: (error: Error) => void;

    let callbacks: { onDraftTransfer: (draft: unknown) => void };
    const conversations: string[] = [];
    const queries: string[] = [];
    const errors: string[] = [];
    const hooks = {
        useState(initial: unknown) {
            const index = cursor++;
            if (!(index in values))
                values[index] = typeof initial === "function" ? initial() : initial;
            return [
                values[index],
                (next: unknown) => {
                    values[index] = typeof next === "function" ? next(values[index]) : next;
                },
            ];
        },
        useRef(initial: unknown) {
            const index = cursor++;
            return (values[index] ??= { current: initial });
        },
        useEffect() {},
    };
    const path = resolve("src/features/ai-assistant/components/ai-chat-drawer.tsx");
    const require = createRequire(path);
    const exports: Record<string, (props: object) => ReactElement> = {};
    const code = transpileModule(readFileSync(path, "utf8"), {
        compilerOptions: { module: ModuleKind.CommonJS, jsx: JsxEmit.ReactJSX },
    }).outputText;
    const component = (name: string) => Object.assign(() => null, { displayName: name });
    const TransferModal = component("TransferModal");
    const DraftTransferCard = component("DraftTransferCard");
    new Function("require", "exports", code)((name: string) => {
        if (name === "react") return hooks;
        if (name === "sonner")
            return { toast: { error: (message: string) => errors.push(message) } };
        if (name.endsWith("ai-stream"))
            return {
                streamAiChat: (query: string, id: string, cb: typeof callbacks) => {
                    queries.push(query);
                    conversations.push(id);
                    callbacks = cb;
                    return new Promise<void>((done, reject) => {
                        finish = done;
                        fail = reject;
                    });
                },
            };
        if (name.endsWith("ai.actions"))
            return {
                clearAiConversationAction: async () => ({ success: true }),
                sendAiChatAction: async () => {
                    throw new Error("Unexpected replay of AI request");
                },
            };
        if (name.endsWith("transfer-modal")) return { TransferModal };
        if (name.endsWith("draft-transfer-card")) return { DraftTransferCard };
        if (name.endsWith("formatted-ai-message"))
            return { FormattedAiMessage: component("Message") };
        return require(name);
    }, exports);
    const render = () => exports.AiChatDrawer({ walletId: "sender", currentBalance: 1000000 });
    const elements = (node: unknown): ReactElement<Record<string, unknown>>[] => {
        if (Array.isArray(node)) return node.flatMap(elements);
        if (!isValidElement(node)) return [];
        const element = node as ReactElement<Record<string, unknown>>;
        return [element, ...elements(element.props.children)];
    };
    const find = (predicate: (element: ReactElement<Record<string, unknown>>) => boolean) => {
        cursor = 0;
        const found = elements(render()).find(predicate);
        assert.ok(found, "expected rendered element");
        return found.props;
    };
    const count = (predicate: (element: ReactElement<Record<string, unknown>>) => boolean) => {
        cursor = 0;
        return elements(render()).filter(predicate).length;
    };
    const click = (props: Record<string, unknown>) => (props.onClick as () => void)();
    click(find((element) => typeof element.props["aria-label"] === "string"));
    return {
        find,
        count,
        click,
        conversations,
        queries,
        errors,
        TransferModal,
        DraftTransferCard,
        reset: () => find((element) => !!element.props.title),
        async draft(payload: unknown) {
            const sending = (
                find(
                    (element) =>
                        typeof element.props.onClick === "function" &&
                        String(element.props.className).includes("text-left shadow")
                ).onClick as () => Promise<void>
            )();
            callbacks.onDraftTransfer(payload);
            finish();
            await sending;
        },
        start() {
            return (
                find(
                    (element) =>
                        typeof element.props.onClick === "function" &&
                        String(element.props.className).includes("text-left shadow")
                ).onClick as () => Promise<void>
            )();
        },
        finish: () => finish(),
        fail: () => fail(new Error("stream unavailable")),
    };
}

const valid = {
    recipientWalletId: "recipient",
    recipientName: "Nam",
    recipientPhoneOrEmail: "0901234567",
    amount: 50000,
    note: "  Tiền ăn trưa\n ",
    fee: 0,
    canProceed: true,
    message: "",
};

test("confirmed AI draft passes exact amount and note to transfer modal", async () => {
    for (const note of [valid.note, "", "   "]) {
        const ui = drawer();
        await ui.draft({ ...valid, note });
        const card = ui.find((element) => element.type === ui.DraftTransferCard);
        (card.onConfirm as (payload: unknown) => void)({ ...valid, note });
        const modal = ui.find((element) => element.type === ui.TransferModal);
        assert.equal(modal.initialAmount, valid.amount);
        assert.equal(modal.initialDescription, note);
    }
});

test("invalid runtime drafts cannot open a transfer", async () => {
    for (const patch of [
        { amount: 0 },
        { amount: -1 },
        { amount: NaN },
        { amount: Infinity },
        { amount: "50000" },
        { amount: 1.5 },
        { amount: Number.MAX_SAFE_INTEGER + 1 },
        { note: null },
        { recipientWalletId: " " },
        { recipientWalletId: "sender" },
        { canProceed: false },
        { canProceed: "true" },
    ]) {
        const ui = drawer();
        await ui.draft(valid);
        const card = ui.find((element) => element.type === ui.DraftTransferCard);
        (card.onConfirm as (payload: unknown) => void)({ ...valid, ...patch });
        assert.equal(ui.errors.length, 1, JSON.stringify(patch));
        assert.equal(
            ui.count((element) => element.type === ui.TransferModal),
            0
        );
    }
});

test("reset rotates conversation and blocks reset during a pending response", async () => {
    const ui = drawer();
    const resetBeforeSending = ui.reset();
    const sending = ui.start();
    assert.equal(ui.reset().disabled, true);
    ui.click(resetBeforeSending);
    ui.click(ui.reset());
    assert.equal(
        ui.count(
            (element) =>
                element.type === "span" &&
                String(element.props.className).includes("text-[10px] text-slate-400 block")
        ),
        2
    );
    ui.finish();
    await sending;
    assert.equal(ui.reset().disabled, false);
    ui.click(ui.reset());
    const next = ui.start();
    assert.notEqual(ui.conversations[0], ui.conversations[1]);
    ui.finish();
    await next;
});

test("transfer form initialization preserves explicit values and manual defaults", async () => {
    const { initializeTransferForm } = await import("../src/features/wallet/transfer-form");
    for (const description of [valid.note, "", "   "]) {
        assert.deepEqual(initializeTransferForm("default", 50000, description), {
            amount: 50000,
            description,
        });
    }
    assert.deepEqual(initializeTransferForm("default"), { amount: 200000, description: "default" });
});

test("stream failure does not replay the request and allows a new conversation", async (context) => {
    context.mock.method(console, "warn", () => {});
    const ui = drawer();
    const sending = ui.start();
    ui.fail();
    await sending;
    assert.equal(ui.conversations.length, 1);
    assert.equal(ui.reset().disabled, false);
    ui.click(ui.reset());
    const next = ui.start();
    assert.notEqual(ui.conversations[0], ui.conversations[1]);
    ui.finish();
    await next;
});
test("completed reset clears the selected transfer and starts a new conversation", async () => {
    const ui = drawer();
    await ui.draft(valid);
    const card = ui.find((element) => element.type === ui.DraftTransferCard);
    (card.onConfirm as (payload: unknown) => void)(valid);
    assert.equal(
        ui.count((element) => element.type === ui.TransferModal),
        1
    );
    ui.click(ui.reset());
    assert.equal(
        ui.count((element) => element.type === ui.TransferModal),
        0
    );
    assert.equal(
        ui.count((element) => element.type === ui.DraftTransferCard),
        0
    );
    await ui.draft({ ...valid, amount: 70000, note: "new note" });
    assert.notEqual(ui.conversations[0], ui.conversations[1]);
});

test("modal keeps the exact draft note while the action retains existing normalization", async () => {
    const { initializeTransferForm } = await import("../src/features/wallet/transfer-form");
    const requests: Record<string, unknown>[] = [];
    const actionPath = resolve("src/features/wallet/actions/wallet.actions.ts");
    const actionExports: Record<string, (...args: unknown[]) => Promise<{ success: boolean }>> = {};
    const compile = (path: string) =>
        transpileModule(readFileSync(path, "utf8"), {
            compilerOptions: { module: ModuleKind.CommonJS, jsx: JsxEmit.ReactJSX },
        }).outputText;
    new Function("require", "exports", compile(actionPath))((name: string) => {
        if (name === "next-intl/server")
            return {
                getTranslations: async () => (key: string) => key,
                getLocale: async () => "vi",
            };
        if (name === "next/cache") return { revalidatePath() {} };
        if (name === "@/server/session") return {};
        if (name === "@/server/backend-client")
            return {
                BackendError: class extends Error {},
                backendFetch: async (_: string, options: { body: string }) => {
                    requests.push(JSON.parse(options.body));
                    return { data: {} };
                },
            };
        return createRequire(actionPath)(name);
    }, actionExports);

    for (const note of [valid.note, "", "   "]) {
        const state: unknown[] = [];
        let cursor = 0;
        const effects: (() => void)[] = [];
        let mount = true;
        let submitted: Record<string, unknown> | undefined;
        const modalPath = resolve("src/features/wallet/components/transfer-modal.tsx");
        const modalExports: Record<string, (props: object) => ReactElement> = {};
        new Function("require", "exports", compile(modalPath))((name: string) => {
            if (name === "react")
                return {
                    useState(initial: unknown) {
                        const index = cursor++;
                        if (!(index in state))
                            state[index] = typeof initial === "function" ? initial() : initial;
                        return [
                            state[index],
                            (value: unknown) => {
                                state[index] = value;
                            },
                        ];
                    },
                    useRef(initial: unknown) {
                        const index = cursor++;
                        return (state[index] ??= { current: initial });
                    },
                    useEffect: (effect: () => void) => {
                        if (mount) effects.push(effect);
                    },
                };
            if (name === "next-intl")
                return {
                    useTranslations: () => (key: string) => key,
                    useLocale: () => "vi",
                };
            if (name === "../transfer-form") return { initializeTransferForm };
            if (name === "./pin-modal") return { PinModal: "PinModal" };
            if (name === "../hooks/use-wallet")
                return {
                    useBeneficiariesQuery: () => ({ data: [] }),
                    useAddBeneficiaryMutation: () => ({}),
                    useTransferMutation: () => ({
                        mutateAsync: async (data: Record<string, unknown>) => {
                            submitted = data;
                        },
                    }),
                };
            if (name === "../actions/wallet.actions") return {};
            if (name === "sonner") return { toast: { success() {} } };
            if (name.startsWith("@/components/"))
                return new Proxy({}, { get: (_, key) => String(key) });
            return createRequire(modalPath)(name);
        }, modalExports);
        const props = {
            isOpen: true,
            onClose() {},
            senderWalletId: "sender",
            currentBalance: 1000000,
            initialRecipient: { identifier: "0901234567", walletId: "recipient", fullName: "Nam" },
            initialAmount: valid.amount,
            initialDescription: note,
        };
        modalExports.TransferModal(props);
        effects.forEach((effect) => effect());
        mount = false;
        cursor = 0;
        const findForm = (
            node: unknown,
            target = "form"
        ): ReactElement<Record<string, unknown>> | undefined => {
            if (Array.isArray(node))
                return node.map((child) => findForm(child, target)).find(Boolean);
            if (!isValidElement(node)) return undefined;
            const element = node as ReactElement<Record<string, unknown>>;
            return element.type === target ? element : findForm(element.props.children, target);
        };
        const form = findForm(modalExports.TransferModal(props));
        assert.ok(form);
        await (form.props.onSubmit as (event: object) => Promise<void>)({ preventDefault() {} });
        assert.equal(submitted, undefined);
        cursor = 0;
        const pinModal = findForm(modalExports.TransferModal(props), "PinModal");
        assert.ok(pinModal);
        await (pinModal.props.onConfirm as (pin: string) => Promise<void>)("123456");
        assert.ok(submitted);
        const transfer = submitted as Record<string, unknown>;
        assert.equal(transfer.amount, valid.amount);
        assert.equal(transfer.description, note);
        assert.equal(transfer.pin, "123456");
        const result = await actionExports.transferMoneyAction(
            "sender",
            "recipient",
            transfer.amount,
            transfer.description,
            transfer.pin
        );
        assert.equal(result.success, true);
        assert.equal(requests.at(-1)?.description, note.trim() || "Wallet transfer");
        assert.equal(requests.at(-1)?.amount, valid.amount);
        assert.equal(requests.at(-1)?.pin, "123456");
    }
});

test("transfer suggestion uses the conservative parser's recipient and note syntax", async () => {
    const ui = drawer();
    const suggestion = ui.find((element) => {
        if (element.type !== "button" || !Array.isArray(element.props.children)) return false;
        return element.props.children.some(
            (child: unknown) =>
                isValidElement<{ children: unknown }>(child) &&
                child.props.children === "💸 Soạn lệnh chuyển tiền"
        );
    });
    const sending = (suggestion.onClick as () => Promise<void>)();
    assert.equal(ui.queries[0], "Chuyển 50.000đ cho Nam nội dung ăn trưa");
    ui.finish();
    await sending;
});
