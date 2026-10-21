export function initializeTransferForm(
    defaultDescription: string,
    initialAmount?: number,
    initialDescription?: string
) {
    return {
        amount: initialAmount ?? 200000,
        description: initialDescription ?? defaultDescription,
    };
}
