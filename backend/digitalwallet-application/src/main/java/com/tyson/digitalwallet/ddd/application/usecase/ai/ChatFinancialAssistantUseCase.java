package com.tyson.digitalwallet.ddd.application.usecase.ai;

import com.tyson.digitalwallet.ddd.application.usecase.ai.command.ChatAssistantCommand;
import com.tyson.digitalwallet.ddd.application.usecase.ai.response.ChatAssistantResponse;

public interface ChatFinancialAssistantUseCase {
    ChatAssistantResponse chat(ChatAssistantCommand command);
}
