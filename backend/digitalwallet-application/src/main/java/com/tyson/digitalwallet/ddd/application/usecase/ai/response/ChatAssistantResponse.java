package com.tyson.digitalwallet.ddd.application.usecase.ai.response;

public record ChatAssistantResponse(
        String answer,
        DraftTransferPayload draftTransfer
) {
    public static ChatAssistantResponse ofText(String answer) {
        return new ChatAssistantResponse(answer, null);
    }
}
