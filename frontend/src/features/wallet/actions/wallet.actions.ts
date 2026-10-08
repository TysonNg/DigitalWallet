"use server";

import { backendFetch, BackendError } from "@/server/backend-client";
import { getSession } from "@/server/session";
import { WalletDto, TransactionDto } from "../types/wallet.types";
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
    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return { success: false, message: "Session has expired" };
        }

        const res = await backendFetch<WalletDto>(`/wallet/user/${session.user.id}`);
        return {
            success: true,
            message: "Retrieved wallet info successfully",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: "Unable to connect to server" };
    }
}

/**
 * Lấy danh sách lịch sử giao dịch của ví.
 */
export async function getWalletTransactionsAction(walletId: string): Promise<ActionResult<TransactionDto[]>> {
    try {
        const res = await backendFetch<TransactionDto[]>(`/wallet/${walletId}/transactions`);
        return {
            success: true,
            message: "Retrieved transaction history successfully",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: "Unable to retrieve transaction history" };
    }
}

/**
 * Nạp tiền vào ví chính.
 */
export async function depositMoneyAction(
    walletId: string,
    amount: number
): Promise<ActionResult<WalletDto>> {
    try {
        if (amount <= 0) {
            return { success: false, message: "Deposit amount must be greater than 0" };
        }

        const res = await backendFetch<WalletDto>("/wallet/deposit", {
            method: "POST",
            body: JSON.stringify({ walletId, amount }),
        });

        revalidatePath("/dashboard");
        return {
            success: true,
            message: "Deposit successful!",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: "Deposit failed" };
    }
}

/**
 * Rút tiền từ ví về tài khoản ngân hàng.
 */
export async function withdrawMoneyAction(
    walletId: string,
    amount: number
): Promise<ActionResult<WalletDto>> {
    try {
        if (amount <= 0) {
            return { success: false, message: "Withdrawal amount must be greater than 0" };
        }

        const res = await backendFetch<WalletDto>("/wallet/withdraw", {
            method: "POST",
            body: JSON.stringify({ walletId, amount }),
        });

        revalidatePath("/dashboard");
        return {
            success: true,
            message: "Withdrawal successful!",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: "Withdrawal failed" };
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
    try {
        if (amount <= 0) {
            return { success: false, message: "Transfer amount must be greater than 0" };
        }
        if (!receiverWalletId || receiverWalletId.trim() === "") {
            return { success: false, message: "Please enter recipient wallet ID" };
        }
        if (senderWalletId === receiverWalletId) {
            return { success: false, message: "Cannot transfer money to your own wallet" };
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
            message: "Transfer successful!",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: "Transfer failed" };
    }
}
