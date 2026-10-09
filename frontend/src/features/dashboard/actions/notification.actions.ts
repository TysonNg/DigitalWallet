"use server";

import { backendFetch } from "@/server/backend-client";
import { NotificationItem } from "../types/notification.types";

/**
 * Lấy danh sách thông báo của người dùng từ Spring Boot Backend
 */
export async function getNotificationsAction() {
    try {
        const res = await backendFetch<NotificationItem[]>("/notifications");
        return {
            success: true,
            data: res.data || [],
        };
    } catch {
        return {
            success: false,
            data: [] as NotificationItem[],
        };
    }
}

/**
 * Đánh dấu toàn bộ thông báo là đã đọc
 */
export async function markAllNotificationsReadAction() {
    try {
        await backendFetch("/notifications/read-all", {
            method: "PUT",
        });
        return { success: true };
    } catch {
        return { success: false };
    }
}
