package com.tyson.digitalwallet.ddd.application.usecase.ai;

import com.tyson.digitalwallet.ddd.application.usecase.ai.command.ChatAssistantCommand;
import com.tyson.digitalwallet.ddd.application.usecase.ai.response.ChatAssistantResponse;

import java.util.UUID;

public interface ChatFinancialAssistantUseCase {
    ChatAssistantResponse chat(ChatAssistantCommand command);
    void clearConversation(UUID userId, String conversationId);
}
