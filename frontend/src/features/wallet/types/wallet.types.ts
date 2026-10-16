export interface WalletDto {
    id: string;
    userId: string;
    balance: number;
    currency: string;
    status: "ACTIVE" | "LOCKED" | "SUSPENDED";
    hasPin?: boolean;
}

export type TransactionType = "DEPOSIT" | "WITHDRAW" | "TRANSFER" | "TRANSFER_IN" | "TRANSFER_OUT";
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

export type PaymentGatewayType = "PAYOS" | "STRIPE";
export type PaymentOrderStatus = "PENDING" | "SUCCESS" | "FAILED" | "CANCELLED" | "EXPIRED";

export interface PaymentOrderDto {
    id: string;
    walletId: string;
    userId: string;
    transactionId?: string | null;
    amount: number;
    gateway: PaymentGatewayType;
    gatewayOrderId?: string | null;
    status: PaymentOrderStatus;
    checkoutUrl?: string | null;
    createdAt: string;
    completedAt?: string | null;
}

export interface RecipientLookupDto {
    userId: string;
    walletId: string;
    fullName: string;
    phoneNumber?: string | null;
    email?: string | null;
    currency: string;
}

export interface BeneficiaryDto {
    id: string;
    userId: string;
    beneficiaryUserId: string;
    beneficiaryWalletId: string;
    nickname?: string | null;
    fullName?: string | null;
    phoneNumber?: string | null;
    email?: string | null;
    createdAt: string;
    updatedAt: string;
}
