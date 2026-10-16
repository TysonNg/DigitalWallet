"use client";

import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/query-keys";
import {
    getNotificationsAction,
    markAllNotificationsReadAction,
} from "../actions/notification.actions";
import { NotificationItem } from "../types/notification.types";

/**
 * Hook truy vấn danh sách thông báo
 */
export function useNotificationsQuery(userId: string) {
    const t = useTranslations("dashboard");

    return useQuery<NotificationItem[]>({
        queryKey: queryKeys.notifications.user(userId),
        enabled: Boolean(userId),
        refetchInterval: 30000,
        queryFn: async () => {
            const res = await getNotificationsAction();
            if (!res.success) throw new Error(t("notificationsLoadError"));
            return res.data ?? [];
        },
    });
}

/**
 * Hook Mutation: Đánh dấu tất cả thông báo là đã đọc
 */
export function useMarkAllReadMutation() {
    const t = useTranslations("dashboard");

    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async () => {
            const res = await markAllNotificationsReadAction();
            if (!res.success) {
                throw new Error(t("notificationsReadError"));
            }
            return res;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all });
        },
    });
}
