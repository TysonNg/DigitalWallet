"use client";

import { useTranslations } from "next-intl";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query-keys";
import {
    getMyWalletAction,
    getWalletTransactionsAction,
    depositMoneyAction,
    withdrawMoneyAction,
    transferMoneyAction,
    createPaymentOrderAction,
    getBeneficiariesAction,
    addBeneficiaryAction,
    updateBeneficiaryAction,
    deleteBeneficiaryAction,
} from "../actions/wallet.actions";
import { WalletDto, TransactionDto, PaymentGatewayType, BeneficiaryDto } from "../types/wallet.types";

/**
 * Hook truy vấn thông tin ví của người dùng hiện tại
 */
export function useMyWalletQuery() {
    const t = useTranslations("walletErrors");

    return useQuery<WalletDto | null>({
        queryKey: queryKeys.wallet.me,
        queryFn: async () => {
            const res = await getMyWalletAction();
            if (!res.success) {
                throw new Error(res.message || t("unableToRetrieveYourWallet"));
            }
            return res.data ?? null;
        },
    });
}

/**
 * Hook truy vấn danh sách lịch sử giao dịch của ví
 */
export function useTransactionsQuery(walletId?: string) {
    const t = useTranslations("walletErrors");

    return useQuery<TransactionDto[]>({
        queryKey: queryKeys.wallet.transactions(walletId),
        queryFn: async () => {
            if (!walletId) return [];
            const res = await getWalletTransactionsAction(walletId);
            if (!res.success) {
                throw new Error(res.message || t("unableToRetrieveTransactions"));
            }
            return res.data ?? [];
        },
        enabled: Boolean(walletId),
    });
}

/**
 * Hook Mutation: Nạp tiền vào ví
 */
export function useDepositMutation() {
    const t = useTranslations("walletErrors");

    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ walletId, amount }: { walletId: string; amount: number }) => {
            const res = await depositMoneyAction(walletId, amount);
            if (!res.success) {
                throw new Error(res.message || t("depositFailed"));
            }
            return res;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.wallet.all });
        },
    });
}

/**
 * Hook Mutation: Rút tiền từ ví
 */
export function useWithdrawMutation() {
    const t = useTranslations("walletErrors");

    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ walletId, amount }: { walletId: string; amount: number }) => {
            const res = await withdrawMoneyAction(walletId, amount);
            if (!res.success) {
                throw new Error(res.message || t("withdrawalFailed"));
            }
            return res;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.wallet.all });
        },
    });
}

/**
 * Hook Mutation: Chuyển tiền giữa các ví
 */
export function useTransferMutation() {
    const t = useTranslations("walletErrors");

    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({
            senderWalletId,
            receiverWalletId,
            amount,
            description,
        }: {
            senderWalletId: string;
            receiverWalletId: string;
            amount: number;
            description: string;
        }) => {
            const res = await transferMoneyAction(
                senderWalletId,
                receiverWalletId,
                amount,
                description
            );
            if (!res.success) {
                throw new Error(res.message || t("transferFailed"));
            }
            return res;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.wallet.all });
        },
    });
}

/**
 * Hook Mutation: Tạo đơn nạp tiền qua cổng thanh toán (PayOS / Stripe)
 */
export function useCreatePaymentOrderMutation() {
    const t = useTranslations("walletErrors");

    return useMutation({
        mutationFn: async ({
            walletId,
            amount,
            gateway,
        }: {
            walletId: string;
            amount: number;
            gateway: PaymentGatewayType;
        }) => {
            const res = await createPaymentOrderAction({ walletId, amount, gateway });
            if (!res.success || !res.data) {
                throw new Error(res.message || t("unableToCreateAPaymentLink"));
            }
            return res.data;
        },
    });
}

/**
 * Hook truy vấn danh sách người thụ hưởng đã lưu
 */
export function useBeneficiariesQuery() {
    return useQuery<BeneficiaryDto[]>({
        queryKey: queryKeys.beneficiaries.all,
        queryFn: async () => {
            const res = await getBeneficiariesAction();
            if (!res.success) {
                throw new Error(res.message || "Không thể tải danh bạ thụ hưởng");
            }
            return res.data ?? [];
        },
    });
}

/**
 * Hook Mutation: Thêm người thụ hưởng mới
 */
export function useAddBeneficiaryMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (params: {
            beneficiaryUserId: string;
            beneficiaryWalletId: string;
            nickname?: string;
        }) => {
            const res = await addBeneficiaryAction(params);
            if (!res.success) {
                throw new Error(res.message || "Thêm người thụ hưởng thất bại");
            }
            return res.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.beneficiaries.all });
        },
    });
}

/**
 * Hook Mutation: Cập nhật tên gợi nhớ
 */
export function useUpdateBeneficiaryMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({
            beneficiaryId,
            nickname,
        }: {
            beneficiaryId: string;
            nickname?: string;
        }) => {
            const res = await updateBeneficiaryAction(beneficiaryId, nickname);
            if (!res.success) {
                throw new Error(res.message || "Cập nhật tên gợi nhớ thất bại");
            }
            return res.data;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.beneficiaries.all });
        },
    });
}

/**
 * Hook Mutation: Xoá người thụ hưởng khỏi danh bạ
 */
export function useDeleteBeneficiaryMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async (beneficiaryId: string) => {
            const res = await deleteBeneficiaryAction(beneficiaryId);
            if (!res.success) {
                throw new Error(res.message || "Xoá người thụ hưởng thất bại");
            }
            return res;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.beneficiaries.all });
        },
    });
}
