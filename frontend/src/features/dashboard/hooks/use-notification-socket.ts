"use client";

import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import { toast } from "sonner";
import { getAccessTokenAction } from "@/features/auth/actions/auth.actions";
import { NotificationItem } from "../types/notification.types";

interface UseNotificationSocketProps {
    onNotificationReceived?: (notification: NotificationItem) => void;
}

export function useNotificationSocket({ onNotificationReceived }: UseNotificationSocketProps = {}) {
    const callbackRef = useRef(onNotificationReceived);
    callbackRef.current = onNotificationReceived;

    useEffect(() => {
        let isMounted = true;
        let client: Client | null = null;

        async function connect() {
            // 1. Lấy JWT Access Token từ BFF Session
            const token = await getAccessTokenAction();
            if (!token || !isMounted) return;

            // 2. Địa chỉ WebSocket kết nối tới Spring Boot
            const wsUrl = process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:3006/api/v1/ws";

            // 3. Khởi tạo Client STOMP
            client = new Client({
                brokerURL: wsUrl,
                connectHeaders: {
                    Authorization: `Bearer ${token}`,
                },
                reconnectDelay: 5000,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
                onConnect: () => {
                    // Subscribe vào hàng đợi thông báo cá nhân của người dùng
                    client?.subscribe("/user/queue/notifications", (message) => {
                        try {
                            const notification: NotificationItem = JSON.parse(message.body);

                            // Hiển thị Toast thông báo realtime bằng Sonner
                            toast.success(notification.title || "Thông báo giao dịch", {
                                description: notification.content,
                                duration: 5000,
                            });

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
            if (client) {
                client.deactivate();
            }
        };
    }, []);
}
