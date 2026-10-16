import type { NotificationItem } from "./types/notification.types";

type NotificationPayload = Omit<NotificationItem, "isRead"> & { isRead?: boolean; read?: boolean };
type Request = <T>(path: string, options?: RequestInit) => Promise<{ data?: T }>;

export function normalizeNotification(item: NotificationPayload): NotificationItem {
    return { ...item, isRead: item.isRead ?? item.read ?? false };
}

export async function loadNotifications(userId: string, request: Request) {
    const response = await request<NotificationPayload[]>(
        `/notifications/user/${encodeURIComponent(userId)}`
    );
    return (response.data ?? []).map(normalizeNotification);
}

export async function readAllNotifications(userId: string, request: Request) {
    await request(`/notifications/user/${encodeURIComponent(userId)}/read-all`, {
        method: "PATCH",
    });
}

export function notificationSocketUrl(origin: string, configured?: string) {
    if (configured) return configured;
    const url = new URL(origin);
    url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
    url.port = "3006";
    url.pathname = "/api/v1/ws";
    return url.toString();
}
