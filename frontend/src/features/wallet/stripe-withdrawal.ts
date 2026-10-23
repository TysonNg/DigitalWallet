import type { WithdrawalInput } from "./types/stripe-withdrawal.types";

export function validateWithdrawal(
    input: WithdrawalInput,
    wallet?: { balance?: number; hasPin?: boolean }
): string | null {
    if (!input.walletId.trim()) return "Wallet is required.";
    if (
        !Number.isSafeInteger(input.amount) ||
        input.amount < 25000 ||
        input.amount > 100000000 ||
        input.amount % 250 !== 0
    ) {
        return "Enter 25,000–100,000,000 VND in increments of 250 VND.";
    }
    if (input.testBank !== "SUCCESS" && input.testBank !== "FAILURE")
        return "Choose a supported test bank.";
    if (wallet?.balance !== undefined && input.amount > wallet.balance)
        return "Insufficient available balance.";
    if ((wallet?.hasPin || input.pin !== undefined) && !/^\d{6}$/.test(input.pin ?? ""))
        return "Enter your six-digit wallet PIN.";

    return null;
}

type IntentStorage = Pick<Storage, "getItem" | "setItem" | "removeItem">;

export function createWithdrawalIntent(
    generateKey: () => string = () => crypto.randomUUID(),
    storage?: IntentStorage
) {
    const keys = new Map<string, string>();
    const store = () => storage ?? (typeof window === "undefined" ? undefined : window.sessionStorage);
    // Only non-secret intent fields form the storage key. PIN is never retained.
    const identity = (input: WithdrawalInput) =>
        `stripe-withdrawal:v1:${JSON.stringify([input.walletId, input.amount, input.testBank])}`;

    const read = (id: string) => store()?.getItem(id) ?? keys.get(id);

    return {
        keyFor(input: WithdrawalInput): string {
            const id = identity(input);
            const key = read(id) ?? generateKey();
            // Persist before the request. If browser storage is blocked, do not
            // submit a request whose retry identity would be lost on reload.
            store()?.setItem(id, key);
            keys.set(id, key);

            return key;
        },
        hasPending(input: WithdrawalInput): boolean {
            try {
                return Boolean(read(identity(input)));
            } catch {
                return keys.has(identity(input));
            }
        },
        confirm(input: WithdrawalInput) {
            const id = identity(input);
            store()?.removeItem(id);
            keys.delete(id);
        },
    };
}
