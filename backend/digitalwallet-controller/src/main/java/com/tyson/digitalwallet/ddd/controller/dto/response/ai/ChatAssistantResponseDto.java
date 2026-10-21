package com.tyson.digitalwallet.ddd.controller.dto.response.ai;

import com.tyson.digitalwallet.ddd.application.usecase.ai.response.ChatAssistantResponse;
import com.tyson.digitalwallet.ddd.application.usecase.ai.response.DraftTransferPayload;

public record ChatAssistantResponseDto(
        String answer,
        DraftTransferPayload draftTransfer
) {
    public static ChatAssistantResponseDto from(ChatAssistantResponse response) {
        return new ChatAssistantResponseDto(response.answer(), response.draftTransfer());
    }
}
