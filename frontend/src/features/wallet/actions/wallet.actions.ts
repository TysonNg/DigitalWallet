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
    errorCode?: string;
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
            return { success: false, message: error.message, errorCode: error.errorCode };
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
            return { success: false, message: error.message, errorCode: error.errorCode };
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
            return { success: false, message: t("theDepositAmountMustBeGreaterThan0"), errorCode: "INVALID_AMOUNT" };
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
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: t("depositFailed") };
    }
}

/**
 * Rút tiền từ ví về tài khoản ngân hàng.
 */
export async function withdrawMoneyAction(
    walletId: string,
    amount: number,
    pin?: string
): Promise<ActionResult<WalletDto>> {
    const t = await getTranslations("walletErrors");

    try {
        if (amount <= 0) {
            return { success: false, message: t("theWithdrawalAmountMustBeGreaterThan0") };
        }

        const res = await backendFetch<WalletDto>("/wallet/withdraw", {
            method: "POST",
            body: JSON.stringify({
                walletId,
                amount,
                pin: pin?.trim() || undefined,
            }),
        });

        revalidatePath("/dashboard");
        return {
            success: true,
            message: t("withdrawalSuccessful"),
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
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
    description: string,
    pin?: string
): Promise<ActionResult<WalletDto>> {
    const t = await getTranslations("walletErrors");

    try {
        if (amount <= 0) {
            return { success: false, message: t("theTransferAmountMustBeGreaterThan0"), errorCode: "INVALID_AMOUNT" };
        }
        if (!receiverWalletId || receiverWalletId.trim() === "") {
            return { success: false, message: t("pleaseEnterTheRecipientWalletID"), errorCode: "INVALID_RECIPIENT_IDENTIFIER" };
        }
        if (senderWalletId === receiverWalletId) {
            return { success: false, message: t("youCannotTransferToYourOwnWallet"), errorCode: "CANNOT_TRANSFER_TO_SELF" };
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
                pin: pin?.trim() || undefined,
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
            return { success: false, message: error.message, errorCode: error.errorCode };
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
            return { success: false, message: t("yourSessionHasExpired"), errorCode: "SESSION_EXPIRED" };
        }

        if (params.amount < 1000) {
            return { success: false, message: t("theMinimumDepositIsVND1000"), errorCode: "INVALID_AMOUNT" };
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
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: t("unableToConnectToThePaymentGateway") };
    }
}

/**
 * Tra cứu thông tin người nhận trước khi chuyển tiền (SĐT / Email / Mã ví)
 */
export async function lookupRecipientAction(
    identifier: string
): Promise<ActionResult<import("../types/wallet.types").RecipientLookupDto>> {
    const t = await getTranslations("walletErrors");

    try {
        if (!identifier || identifier.trim() === "") {
            return { success: false, message: t("pleaseEnterTheRecipientWalletID"), errorCode: "INVALID_RECIPIENT_IDENTIFIER" };
        }

        const encoded = encodeURIComponent(identifier.trim());
        const res = await backendFetch<import("../types/wallet.types").RecipientLookupDto>(
            `/wallet/lookup?identifier=${encoded}`
        );

        return {
            success: true,
            message: "Tra cứu thông tin người nhận thành công",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: "Không tìm thấy người nhận hoặc lỗi hệ thống" };
    }
}

/**
 * Lấy danh sách danh bạ người thụ hưởng của người dùng hiện tại
 */
export async function getBeneficiariesAction(): Promise<
    ActionResult<import("../types/wallet.types").BeneficiaryDto[]>
> {
    try {
        const res = await backendFetch<import("../types/wallet.types").BeneficiaryDto[]>(
            "/beneficiaries"
        );
        return {
            success: true,
            message: "Lấy danh bạ thụ hưởng thành công",
            data: res.data ?? [],
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: "Không thể lấy danh bạ thụ hưởng" };
    }
}

/**
 * Thêm người thụ hưởng mới vào danh bạ
 */
export async function addBeneficiaryAction(params: {
    beneficiaryUserId: string;
    beneficiaryWalletId: string;
    nickname?: string;
}): Promise<ActionResult<import("../types/wallet.types").BeneficiaryDto>> {
    try {
        const res = await backendFetch<import("../types/wallet.types").BeneficiaryDto>(
            "/beneficiaries",
            {
                method: "POST",
                body: JSON.stringify({
                    beneficiaryUserId: params.beneficiaryUserId,
                    beneficiaryWalletId: params.beneficiaryWalletId,
                    nickname: params.nickname?.trim() || undefined,
                }),
            }
        );
        return {
            success: true,
            message: "Đã thêm vào danh bạ thụ hưởng",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: "Thêm người thụ hưởng thất bại" };
    }
}

/**
 * Cập nhật tên gợi nhớ của người thụ hưởng
 */
export async function updateBeneficiaryAction(
    beneficiaryId: string,
    nickname?: string
): Promise<ActionResult<import("../types/wallet.types").BeneficiaryDto>> {
    try {
        const res = await backendFetch<import("../types/wallet.types").BeneficiaryDto>(
            `/beneficiaries/${beneficiaryId}`,
            {
                method: "PUT",
                body: JSON.stringify({
                    nickname: nickname?.trim() || undefined,
                }),
            }
        );
        return {
            success: true,
            message: "Cập nhật tên gợi nhớ thành công",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: "Cập nhật tên gợi nhớ thất bại" };
    }
}

/**
 * Xoá người thụ hưởng khỏi danh bạ
 */
export async function deleteBeneficiaryAction(
    beneficiaryId: string
): Promise<ActionResult<void>> {
    try {
        await backendFetch<void>(`/beneficiaries/${beneficiaryId}`, {
            method: "DELETE",
        });
        return {
            success: true,
            message: "Xoá người thụ hưởng thành công",
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: "Xoá người thụ hưởng thất bại" };
    }
}

/**
 * Thiết lập mã PIN giao dịch cho ví (6 chữ số).
 */
export async function setupWalletPinAction(
    pin: string,
    walletId?: string
): Promise<ActionResult<void>> {
    let tSuccess = "Thiết lập mã PIN giao dịch thành công!";
    let tFail = "Thiết lập mã PIN thất bại";
    try {
        const t = await getTranslations("walletErrors");
        tSuccess = t("pinSetupSuccess") || tSuccess;
        tFail = t("pinSetupFailed") || tFail;
    } catch {
        // Fallback for tests without Next.js async storage
    }

    try {
        if (!/^[0-9]{6}$/.test(pin)) {
            return {
                success: false,
                message: "Mã PIN phải bao gồm đúng 6 chữ số",
                errorCode: "VALIDATION_ERROR",
            };
        }

        await backendFetch<void>("/wallet/pin/setup", {
            method: "POST",
            body: JSON.stringify({
                walletId: walletId || undefined,
                pin,
            }),
        });

        try {
            revalidatePath("/dashboard");
            revalidatePath("/dashboard/settings");
        } catch {
            // Ignore in tests
        }

        return {
            success: true,
            message: tSuccess,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: tFail };
    }
}

/**
 * Đổi mã PIN giao dịch cho ví.
 */
export async function changeWalletPinAction(
    oldPin: string,
    newPin: string,
    walletId?: string
): Promise<ActionResult<void>> {
    let tSuccess = "Đổi mã PIN giao dịch thành công!";
    let tFail = "Đổi mã PIN thất bại";
    try {
        const t = await getTranslations("walletErrors");
        tSuccess = t("pinChangeSuccess") || tSuccess;
        tFail = t("pinChangeFailed") || tFail;
    } catch {
        // Fallback for tests without Next.js async storage
    }

    try {
        if (!/^[0-9]{6}$/.test(oldPin) || !/^[0-9]{6}$/.test(newPin)) {
            return {
                success: false,
                message: "Mã PIN phải bao gồm đúng 6 chữ số",
                errorCode: "VALIDATION_ERROR",
            };
        }

        if (oldPin === newPin) {
            return {
                success: false,
                message: "Mã PIN mới không được trùng với mã PIN hiện tại",
                errorCode: "NEW_PASSWORD_SAME_AS_OLD",
            };
        }

        await backendFetch<void>("/wallet/pin/change", {
            method: "POST",
            body: JSON.stringify({
                walletId: walletId || undefined,
                oldPin,
                newPin,
            }),
        });

        try {
            revalidatePath("/dashboard");
            revalidatePath("/dashboard/settings");
        } catch {
            // Ignore in tests
        }

        return {
            success: true,
            message: tSuccess,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message, errorCode: error.errorCode };
        }
        return { success: false, message: tFail };
    }
}
