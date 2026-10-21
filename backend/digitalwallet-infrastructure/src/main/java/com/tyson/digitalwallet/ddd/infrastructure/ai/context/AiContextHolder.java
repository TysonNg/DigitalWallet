package com.tyson.digitalwallet.ddd.infrastructure.ai.context;

import com.tyson.digitalwallet.ddd.application.usecase.ai.response.DraftTransferPayload;

import java.util.UUID;

public final class AiContextHolder {

    private static final ThreadLocal<UUID> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<DraftTransferPayload> DRAFT_TRANSFER = new ThreadLocal<>();

    private AiContextHolder() {
    }

    public static void setCurrentUserId(UUID userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static UUID getCurrentUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void setDraftTransfer(DraftTransferPayload payload) {
        DRAFT_TRANSFER.set(payload);
    }

    public static DraftTransferPayload getDraftTransfer() {
        return DRAFT_TRANSFER.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
        DRAFT_TRANSFER.remove();
    }
}
