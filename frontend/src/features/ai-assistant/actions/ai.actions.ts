"use server";

import { backendFetch, BackendError } from "@/server/backend-client";
import { getSession } from "@/server/session";
import { ChatAssistantResponse } from "../types/ai.types";

export interface ActionResult<T> {
    success: boolean;
    message: string;
    data?: T;
}

export async function sendAiChatAction(
    message: string,
    conversationId?: string
): Promise<ActionResult<ChatAssistantResponse>> {
    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return { success: false, message: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại." };
        }

        const res = await backendFetch<ChatAssistantResponse>("/ai/chat", {
            method: "POST",
            body: JSON.stringify({ message, conversationId }),
        });

        return {
            success: true,
            message: "Thành công",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: "Không thể kết nối với dịch vụ Trợ lý AI." };
    }
}

export async function syncAiKnowledgeAction(): Promise<ActionResult<{ ingestedChunks: number }>> {
    try {
        const res = await backendFetch<{ ingestedChunks: number }>("/ai/knowledge/sync", {
            method: "POST",
        });
        return {
            success: true,
            message: "Đồng bộ tri thức thành công",
            data: res.data,
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: "Không thể đồng bộ dữ liệu tri thức." };
    }
}

export async function clearAiConversationAction(
    conversationId: string
): Promise<ActionResult<void>> {
    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return { success: false, message: "Phiên đăng nhập đã hết hạn." };
        }

        await backendFetch<void>(`/ai/chat/conversation/${encodeURIComponent(conversationId)}`, {
            method: "DELETE",
        });

        return {
            success: true,
            message: "Đã xóa ngữ cảnh hội thoại thành công",
        };
    } catch (error) {
        if (error instanceof BackendError) {
            return { success: false, message: error.message };
        }
        return { success: false, message: "Không thể xóa ngữ cảnh hội thoại." };
    }
}

