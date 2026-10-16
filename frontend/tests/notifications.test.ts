import test from "node:test";
import assert from "node:assert/strict";
import {
    loadNotifications,
    readAllNotifications,
    normalizeNotification,
    notificationSocketUrl,
} from "../src/features/dashboard/notification-client";
import type { NotificationItem } from "../src/features/dashboard/types/notification.types";
const item: NotificationItem = {
    id: "n1",
    userId: "u1",
    title: "Deposit",
    content: "Credited",
    type: "DEPOSIT",
    isRead: false,
    createdAt: "2026-10-15T12:00:00",
};
test("notification history and read-all use backend user routes and PATCH", async () => {
    const calls: Array<[string, string | undefined]> = [];
    const request = async <T>(path: string, init?: RequestInit): Promise<{ data: T }> => {
        calls.push([path, init?.method]);
        return { data: [item] as T };
    };
    assert.deepEqual(await loadNotifications("u1", request), [item]);
    await readAllNotifications("u1", request);
    assert.deepEqual(calls, [
        ["/notifications/user/u1", undefined],
        ["/notifications/user/u1/read-all", "PATCH"],
    ]);
});
test("history failures propagate instead of pretending the inbox is empty", async () => {
    await assert.rejects(
        loadNotifications("u1", async () => {
            throw new Error("Backend offline");
        }),
        /Backend offline/
    );
});
test("REST isRead and domain socket read are normalized consistently", () => {
    assert.equal(normalizeNotification({ ...item, isRead: true }).isRead, true);
    const socketItem = { ...item, isRead: undefined };
    assert.equal(normalizeNotification({ ...socketItem, read: true }).isRead, true);
    assert.equal(normalizeNotification({ ...socketItem, read: false }).isRead, false);
});
test("socket defaults to current browser host and preserves explicit endpoints", () => {
    assert.equal(
        notificationSocketUrl("http://192.168.1.20:3000"),
        "ws://192.168.1.20:3006/api/v1/ws"
    );
    assert.equal(
        notificationSocketUrl("https://wallet.example", "wss://api.example/ws"),
        "wss://api.example/ws"
    );
});
