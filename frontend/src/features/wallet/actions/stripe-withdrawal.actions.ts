"use server";

import { backendFetch, BackendError } from "@/server/backend-client";
import { validateWithdrawal } from "../stripe-withdrawal";
import type {
    WithdrawalDto,
    WithdrawalRequest,
    WithdrawalResult,
} from "../types/stripe-withdrawal.types";

export async function createStripeWithdrawalAction(
    input: WithdrawalRequest
): Promise<WithdrawalResult<WithdrawalDto>> {
    const validation = validateWithdrawal(input);
    if (
        validation ||
        !/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
            input.idempotencyKey
        )
    ) {
        return { success: false, message: validation ?? "A valid request UUID is required." };
    }

    try {
        const response = await backendFetch<WithdrawalDto>("/wallet/withdrawals", {
            method: "POST",
            body: JSON.stringify(input),
            cache: "no-store",
            signal: AbortSignal.timeout(20000),
        });
        if (!response.success || !response.data?.id || response.data.mode !== "STRIPE_TEST") {
            return {
                success: false,
                message: "Invalid sandbox withdrawal response. Retry with the same request.",
            };
        }

        return { success: true, data: response.data };
    } catch (error) {
        return {
            success: false,
            message:
                error instanceof BackendError
                    ? error.message
                    : "Unable to confirm the withdrawal. Check history or retry the same request.",
            errorCode: error instanceof BackendError ? error.errorCode : undefined,
        };
    }
}

export async function getStripeWithdrawalsAction(
    walletId: string
): Promise<WithdrawalResult<WithdrawalDto[]>> {
    if (!walletId.trim()) return { success: false, message: "Wallet is required." };

    try {
        const response = await backendFetch<WithdrawalDto[]>(
            `/wallet/withdrawals?walletId=${encodeURIComponent(walletId)}`,
            {
                cache: "no-store",
                signal: AbortSignal.timeout(10000),
            }
        );
        if (!response.success || !Array.isArray(response.data))
            return { success: false, message: "Invalid withdrawal history response." };

        return { success: true, data: response.data };
    } catch (error) {
        return {
            success: false,
            message:
                error instanceof BackendError
                    ? error.message
                    : "Unable to load sandbox withdrawals.",
            errorCode: error instanceof BackendError ? error.errorCode : undefined,
        };
    }
}
