package com.tyson.digitalwallet.ddd.application.usecase.ai.command;

import java.util.UUID;

public record ChatAssistantCommand(
        UUID userId,
        String message,
        String conversationId
) {
}
