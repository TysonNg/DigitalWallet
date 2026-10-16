"use server";

import { backendFetch } from "@/server/backend-client";
import { getSession } from "@/server/session";
import { loadNotifications, readAllNotifications } from "../notification-client";

export async function getNotificationsAction() {
    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) return { success: false, data: [] };
        const data = await loadNotifications(session.user.id, backendFetch);
        return { success: true, data };
    } catch {
        return { success: false, data: [] };
    }
}

export async function markAllNotificationsReadAction() {
    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) return { success: false };
        await readAllNotifications(session.user.id, backendFetch);
        return { success: true };
    } catch {
        return { success: false };
    }
}
