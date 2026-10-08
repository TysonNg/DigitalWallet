export interface WalletDto {
    id: string;
    userId: string;
    balance: number;
    currency: string;
    status: "ACTIVE" | "LOCKED" | "SUSPENDED";
}

export type TransactionType = "DEPOSIT" | "WITHDRAW" | "TRANSFER";
export type TransactionStatus = "PENDING" | "SUCCESS" | "FAILED";

export interface TransactionDto {
    id: string;
    walletId: string;
    senderWalletId?: string | null;
    receiverWalletId?: string | null;
    amount: number;
    type: TransactionType;
    status: TransactionStatus;
    description?: string | null;
    createdAt: string;
    idempotencyKey?: string | null;
}
