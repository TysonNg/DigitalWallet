"use client";

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
export function useNotificationsQuery() {
    return useQuery<NotificationItem[]>({
        queryKey: queryKeys.notifications.all,
        queryFn: async () => {
            const res = await getNotificationsAction();
            return res.data ?? [];
        },
    });
}

/**
 * Hook Mutation: Đánh dấu tất cả thông báo là đã đọc
 */
export function useMarkAllReadMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async () => {
            const res = await markAllNotificationsReadAction();
            if (!res.success) {
                throw new Error("Failed to mark notifications as read");
            }
            return res;
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: queryKeys.notifications.all });
        },
    });
}
