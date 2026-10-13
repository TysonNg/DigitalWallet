"use server";

import { getTranslations, getLocale } from "next-intl/server";

import { backendFetch, BackendError } from "@/server/backend-client";
import { getSession } from "@/server/session";
import {
    WalletDto,
    TransactionDto,
    PaymentGatewayType,
    PaymentOrderDto,
} from "../types/wallet.types";
import { revalidatePath } from "next/cache";

export interface ActionResult<T> {
    success: boolean;
    message: string;
    data?: T;
}

/**
 * Lấy thông tin ví của người dùng hiện tại từ Backend.
 */
export async function getMyWalletAction(): Promise<ActionResult<WalletDto>> {
    const t = await getTranslations("walletErrors");

    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return { success: false, message: t("yourSessionHasExpired") };
        }

        const res = await backendFetch<WalletDto>(`/wallet/user/${session.user.id}`);
        return {
            success: true,
            message: t("walletRetrievedSuccessfully"),
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: t("unableToConnectToTheServer") };
    }
}

/**
 * Lấy danh sách lịch sử giao dịch của ví.
 */
export async function getWalletTransactionsAction(
    walletId: string
): Promise<ActionResult<TransactionDto[]>> {
    const t = await getTranslations("walletErrors");

    try {
        const res = await backendFetch<TransactionDto[]>(`/wallet/${walletId}/transactions`);
        return {
            success: true,
            message: t("transactionHistoryRetrievedSuccessfully"),
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: t("unableToRetrieveTransactionHistory") };
    }
}

/**
 * Nạp tiền vào ví chính.
 */
export async function depositMoneyAction(
    walletId: string,
    amount: number
): Promise<ActionResult<WalletDto>> {
    const t = await getTranslations("walletErrors");

    try {
        if (amount <= 0) {
            return { success: false, message: t("theDepositAmountMustBeGreaterThan0") };
        }

        let targetWalletId = walletId;
        if (!targetWalletId || targetWalletId.trim() === "") {
            const session = await getSession();
            if (session.isLoggedIn && session.user) {
                const walletRes = await backendFetch<WalletDto>(`/wallet/user/${session.user.id}`);
                if (walletRes.data?.id) {
                    targetWalletId = walletRes.data.id;
                }
            }
        }

        const res = await backendFetch<WalletDto>("/wallet/deposit", {
            method: "POST",
            body: JSON.stringify({ walletId: targetWalletId, amount }),
        });

        revalidatePath(`/${await getLocale()}/dashboard`);
        return {
            success: true,
            message: t("depositSuccessful"),
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: t("depositFailed") };
    }
}

/**
 * Rút tiền từ ví về tài khoản ngân hàng.
 */
export async function withdrawMoneyAction(
    walletId: string,
    amount: number
): Promise<ActionResult<WalletDto>> {
    const t = await getTranslations("walletErrors");

    try {
        if (amount <= 0) {
            return { success: false, message: t("theWithdrawalAmountMustBeGreaterThan0") };
        }

        const res = await backendFetch<WalletDto>("/wallet/withdraw", {
            method: "POST",
            body: JSON.stringify({ walletId, amount }),
        });

        revalidatePath("/dashboard");
        return {
            success: true,
            message: t("withdrawalSuccessful"),
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: t("withdrawalFailed") };
    }
}

/**
 * Chuyển tiền từ ví này sang ví khác có Idempotency-Key.
 */
export async function transferMoneyAction(
    senderWalletId: string,
    receiverWalletId: string,
    amount: number,
    description: string
): Promise<ActionResult<WalletDto>> {
    const t = await getTranslations("walletErrors");

    try {
        if (amount <= 0) {
            return { success: false, message: t("theTransferAmountMustBeGreaterThan0") };
        }
        if (!receiverWalletId || receiverWalletId.trim() === "") {
            return { success: false, message: t("pleaseEnterTheRecipientWalletID") };
        }
        if (senderWalletId === receiverWalletId) {
            return { success: false, message: t("youCannotTransferToYourOwnWallet") };
        }

        const idempotencyKey = crypto.randomUUID();

        const res = await backendFetch<WalletDto>("/wallet/transfer", {
            method: "POST",
            body: JSON.stringify({
                senderWalletId,
                receiverWalletId: receiverWalletId.trim(),
                amount,
                description: description.trim() || "Wallet transfer",
                idempotencyKey,
            }),
            headers: {
                "Idempotency-Key": idempotencyKey,
            },
        });

        revalidatePath("/dashboard");
        return {
            success: true,
            message: t("transferSuccessful"),
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: t("transferFailed") };
    }
}

/**
 * Tạo đơn nạp tiền qua cổng thanh toán (PayOS hoặc Stripe).
 */
export async function createPaymentOrderAction(params: {
    walletId: string;
    amount: number;
    gateway: PaymentGatewayType;
}): Promise<ActionResult<PaymentOrderDto>> {
    const t = await getTranslations("walletErrors");

    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return { success: false, message: t("yourSessionHasExpired") };
        }

        if (params.amount < 1000) {
            return { success: false, message: t("theMinimumDepositIsVND1000") };
        }

        let walletId = params.walletId;
        if (!walletId || walletId.trim() === "") {
            const walletRes = await backendFetch<WalletDto>(`/wallet/user/${session.user.id}`);
            if (walletRes.data?.id) {
                walletId = walletRes.data.id;
            }
        }

        const res = await backendFetch<PaymentOrderDto>("/payments/create-order", {
            method: "POST",
            body: JSON.stringify({
                walletId: walletId || undefined,
                userId: session.user.id,
                amount: params.amount,
                gateway: params.gateway,
            }),
        });

        return {
            success: true,
            message: t("paymentLinkCreatedSuccessfully"),
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: t("unableToConnectToThePaymentGateway") };
    }
}
