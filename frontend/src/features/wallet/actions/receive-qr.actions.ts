"use server";

import { getTranslations } from "next-intl/server";
import { backendFetch, BackendError } from "@/server/backend-client";
import { getSession } from "@/server/session";
import { isReceiveUuid, type ReceiveQr, type ReceiveRecipient } from "../receive-qr";
import type { ActionResult } from "./wallet.actions";

async function receiveRequest<T>(
    endpoint: string,
    method: "POST" | "GET"
): Promise<ActionResult<T>> {
    const t = await getTranslations("receiveQr");

    try {
        const session = await getSession();
        if (!session.isLoggedIn || !session.user) {
            return { success: false, message: t("loginRequired"), errorCode: "SESSION_EXPIRED" };
        }

        const response = await backendFetch<T>(endpoint, { method, cache: "no-store" });

        return { success: true, message: "", data: response.data };
    } catch (error) {
        return {
            success: false,
            message: error instanceof BackendError ? error.message : t("failed"),
            errorCode:
                error instanceof BackendError && error.statusCode === 401
                    ? "SESSION_EXPIRED"
                    : error instanceof BackendError
                      ? error.errorCode
                      : undefined,
        };
    }
}

export async function getReceiveQrAction(): Promise<ActionResult<ReceiveQr>> {
    return receiveRequest<ReceiveQr>("/wallet/receive-qr", "POST");
}

export async function resolveReceiveRecipientAction(
    token: string
): Promise<ActionResult<ReceiveRecipient>> {
    if (!isReceiveUuid(token)) {
        const t = await getTranslations("receiveQr");

        return { success: false, message: t("invalid"), errorCode: "INVALID_RECEIVE_TOKEN" };
    }

    return receiveRequest<ReceiveRecipient>(`/wallet/receive-qr/${token}/recipient`, "GET");
}
