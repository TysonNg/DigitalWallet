"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query-keys";
import {
    getMyWalletAction,
    getWalletTransactionsAction,
    depositMoneyAction,
    withdrawMoneyAction,
    transferMoneyAction,
    createPaymentOrderAction,
} from "../actions/wallet.actions";
import { WalletDto, TransactionDto, PaymentGatewayType } from "../types/wallet.types";

/**
 * Hook truy vấn thông tin ví của người dùng hiện tại
 */
export function useMyWalletQuery() {
    return useQuery<WalletDto | null>({
        queryKey: queryKeys.wallet.me,
        queryFn: async () => {
            const res = await getMyWalletAction();
            if (!res.success) {
                throw new Error(res.message || "Failed to fetch wallet");
            }
            return res.data ?? null;
        },
    });
}

/**
 * Hook truy vấn danh sách lịch sử giao dịch của ví
 */
export function useTransactionsQuery(walletId?: string) {
    return useQuery<TransactionDto[]>({
        queryKey: queryKeys.wallet.transactions(walletId),
        queryFn: async () => {
            if (!walletId) return [];
            const res = await getWalletTransactionsAction(walletId);
            if (!res.success) {
                throw new Error(res.message || "Failed to fetch transactions");
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
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ walletId, amount }: { walletId: string; amount: number }) => {
            const res = await depositMoneyAction(walletId, amount);
            if (!res.success) {
                throw new Error(res.message || "Deposit failed");
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
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async ({ walletId, amount }: { walletId: string; amount: number }) => {
            const res = await withdrawMoneyAction(walletId, amount);
            if (!res.success) {
                throw new Error(res.message || "Withdrawal failed");
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
                throw new Error(res.message || "Transfer failed");
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
                throw new Error(res.message || "Failed to create payment link");
            }
            return res.data;
        },
    });
}

