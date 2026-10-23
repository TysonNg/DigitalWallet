"use client";

import { useEffect, useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query-keys";
import {
    createStripeWithdrawalAction,
    getStripeWithdrawalsAction,
} from "../actions/stripe-withdrawal.actions";
import type { WithdrawalDto, WithdrawalRequest } from "../types/stripe-withdrawal.types";

const withdrawalsKey = (walletId: string) => ["stripe-withdrawals", walletId] as const;

export function useStripeWithdrawalsQuery(walletId: string) {
    const client = useQueryClient();

    const previous = useRef<{ walletId: string; signature: string } | undefined>(undefined);

    const query = useQuery({
        queryKey: withdrawalsKey(walletId),
        queryFn: async () => {
            const result = await getStripeWithdrawalsAction(walletId);
            if (!result.success) throw new Error(result.message);

            return result.data;
        },
        enabled: Boolean(walletId),
        refetchInterval: 3000,
        retry: false,
    });

    useEffect(() => {
        if (!query.data) return;
        const signature = query.data
            .map((item) => `${item.id}:${item.status}:${item.updatedAt}`)
            .join("|");
        if (previous.current?.walletId === walletId && previous.current.signature !== signature) {
            void client.invalidateQueries({ queryKey: queryKeys.wallet.all });
        }
        previous.current = { walletId, signature };
    }, [query.data, walletId, client]);

    return query;
}

export function useStripeWithdrawalMutation() {
    const client = useQueryClient();

    return useMutation({
        mutationFn: async (input: WithdrawalRequest) => {
            const result = await createStripeWithdrawalAction(input);
            if (!result.success) throw new Error(result.message);

            return result.data;
        },
        retry: false,
        onSuccess: (withdrawal) => {
            client.setQueryData<WithdrawalDto[]>(withdrawalsKey(withdrawal.walletId), (old) =>
                [withdrawal, ...(old ?? []).filter((item) => item.id !== withdrawal.id)].slice(
                    0,
                    50
                )
            );
            void client.invalidateQueries({ queryKey: queryKeys.wallet.all });
            void client.invalidateQueries({ queryKey: withdrawalsKey(withdrawal.walletId) });
        },
    });
}
