export type NotificationType = "TRANSFER" | "DEPOSIT" | "WITHDRAW" | "SYSTEM";

export interface NotificationItem {
    id: string;
    userId: string;
    title: string;
    content: string;
    transactionId?: string;
    type: NotificationType;
    isRead: boolean;
    readAt?: string | null;
    createdAt: string;
}
