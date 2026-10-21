import { DraftTransferPayload } from "../types/ai.types";

export interface StreamCallbacks {
    onToken: (token: string) => void;
    onDraftTransfer: (draft: DraftTransferPayload) => void;
    onError: (error: string) => void;
    onDone: () => void;
}

/**
 * Consumes the SSE stream from `/api/ai/chat/stream`.
 * Parses lines with `event:` and `data:` protocols.
 */
export async function streamAiChat(
    message: string,
    conversationId: string,
    callbacks: StreamCallbacks,
    signal?: AbortSignal
): Promise<void> {
    const res = await fetch("/api/ai/chat/stream", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ message, conversationId }),
        signal,
    });

    if (!res.ok) {
        let errMessage = `Lỗi kết nối streaming (${res.status})`;
        try {
            const errJson = await res.json();
            if (errJson.error) errMessage = errJson.error;
        } catch {
            const errText = await res.text().catch(() => "");
            if (errText) errMessage = errText;
        }
        throw new Error(errMessage);
    }

    if (!res.body) {
        throw new Error("Không nhận được dữ liệu stream từ máy chủ.");
    }

    const reader = res.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";

    try {
        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });
            const blocks = buffer.split("\n\n");
            buffer = blocks.pop() || "";

            for (const block of blocks) {
                if (!block.trim()) continue;

                let eventType = "message";
                let dataStr = "";

                const lines = block.split("\n");
                for (const line of lines) {
                    if (line.startsWith("event:")) {
                        eventType = line.slice(6).trim();
                    } else if (line.startsWith("data:")) {
                        dataStr = line.slice(5).trim();
                    }
                }

                if (!dataStr) continue;

                if (eventType === "token") {
                    try {
                        const parsed = JSON.parse(dataStr);
                        if (parsed.content) {
                            callbacks.onToken(parsed.content);
                        }
                    } catch {
                        callbacks.onToken(dataStr);
                    }
                } else if (eventType === "draft_transfer") {
                    try {
                        const draft: DraftTransferPayload = JSON.parse(dataStr);
                        callbacks.onDraftTransfer(draft);
                    } catch (e) {
                        console.error("Failed to parse draft transfer payload:", e);
                    }
                } else if (eventType === "error") {
                    try {
                        const errObj = JSON.parse(dataStr);
                        callbacks.onError(errObj.error || "Lỗi xử lý AI");
                    } catch {
                        callbacks.onError(dataStr);
                    }
                } else if (eventType === "done") {
                    callbacks.onDone();
                    return;
                }
            }
        }
    } finally {
        callbacks.onDone();
    }
}
