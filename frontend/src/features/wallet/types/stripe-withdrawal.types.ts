export type TestBank = "SUCCESS" | "FAILURE";

export type WithdrawalStatus =
    "PENDING" | "PROCESSING" | "SUCCEEDED" | "FAILED" | "RETURNED" | "REVIEW_REQUIRED";

export interface WithdrawalDto {
    id: string;
    walletId: string;
    amount: number;
    payoutAmount: number;
    payoutCurrency: "USD";
    mode: "STRIPE_TEST";
    testBank: TestBank;
    status: WithdrawalStatus;
    stripeAccountId?: string;
    transferId?: string;
    payoutId?: string;
    error?: string;
    createdAt: string;
    updatedAt: string;
}

export interface WithdrawalInput {
    walletId: string;
    amount: number;
    testBank: TestBank;
    pin?: string;
}

export interface WithdrawalRequest extends WithdrawalInput {
    idempotencyKey: string;
}

export type WithdrawalResult<T> =
    { success: true; data: T } | { success: false; message: string; errorCode?: string };
