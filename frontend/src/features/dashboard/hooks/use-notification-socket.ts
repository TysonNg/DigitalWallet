"use client";

import { useTranslations } from "next-intl";
import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import { toast } from "sonner";
import { getAccessTokenAction } from "@/features/auth/actions/auth.actions";
import { NotificationItem } from "../types/notification.types";

import { normalizeNotification, notificationSocketUrl } from "../notification-client";

interface UseNotificationSocketProps {
    userId: string;
    onConnected?: () => void;
    onNotificationReceived?: (notification: NotificationItem) => void;
}

export function useNotificationSocket({
    userId,
    onConnected,
    onNotificationReceived,
}: UseNotificationSocketProps) {
    const t = useTranslations("dashboard");
    const translationRef = useRef(t);
    useEffect(() => {
        translationRef.current = t;
    }, [t]);
    const connectedRef = useRef(onConnected);
    useEffect(() => {
        connectedRef.current = onConnected;
    }, [onConnected]);

    const callbackRef = useRef(onNotificationReceived);
    useEffect(() => {
        callbackRef.current = onNotificationReceived;
    }, [onNotificationReceived]);

    useEffect(() => {
        let isMounted = true;
        let client: Client | null = null;
        let retryTimer: ReturnType<typeof setTimeout> | undefined;

        function connect() {
            const wsUrl = notificationSocketUrl(
                window.location.origin,
                process.env.NEXT_PUBLIC_WS_URL
            );

            // 3. Khởi tạo Client STOMP
            client = new Client({
                brokerURL: wsUrl,
                beforeConnect: async () => {
                    try {
                        const token = await getAccessTokenAction();
                        if (!isMounted || !token) {
                            await client?.deactivate();
                            return;
                        }
                        if (client) client.connectHeaders = { Authorization: `Bearer ${token}` };
                    } catch {
                        await client?.deactivate();
                        if (isMounted) {
                            retryTimer = setTimeout(() => {
                                if (isMounted) client?.activate();
                            }, 5000);
                        }
                    }
                },
                reconnectDelay: 5000,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
                onConnect: () => {
                    connectedRef.current?.();
                    // Subscribe vào hàng đợi thông báo cá nhân của người dùng
                    client?.subscribe("/user/queue/notifications", (message) => {
                        try {
                            const notification = normalizeNotification(JSON.parse(message.body));
                            if (notification.userId !== userId) return;

                            // Hiển thị Toast thông báo realtime bằng Sonner
                            toast.success(
                                notification.title ||
                                    translationRef.current("transactionNotifications"),
                                {
                                    description: notification.content,
                                    duration: 5000,
                                }
                            );

                            // Gọi callback để cập nhật UI (số dư, danh sách)
                            if (callbackRef.current) {
                                callbackRef.current(notification);
                            }
                        } catch (err) {
                            console.error("[WebSocket] Lỗi đọc tin nhắn thông báo:", err);
                        }
                    });
                },
                onStompError: (frame) => {
                    console.error("[WebSocket] Lỗi STOMP Broker:", frame.headers["message"]);
                },
            });

            client.activate();
        }

        connect();

        return () => {
            isMounted = false;
            if (retryTimer) clearTimeout(retryTimer);
            if (client) {
                client.deactivate();
            }
        };
    }, [userId]);
}
