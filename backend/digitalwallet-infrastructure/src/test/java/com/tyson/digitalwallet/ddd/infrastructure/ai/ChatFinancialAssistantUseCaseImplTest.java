package com.tyson.digitalwallet.ddd.infrastructure.ai;

import com.tyson.digitalwallet.ddd.application.port.out.ai.VectorKnowledgePort;
import com.tyson.digitalwallet.ddd.application.usecase.ai.command.ChatAssistantCommand;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.GetWalletUseCase;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.infrastructure.ai.service.*;
import com.tyson.digitalwallet.ddd.infrastructure.ai.tools.FinancialAgentTools;
import org.junit.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

public class ChatFinancialAssistantUseCaseImplTest {
    @Test
    public void sameClientConversationId_isIsolatedBetweenUsersAndRemembersOwnHistory() {
        ChatModel model = mock(ChatModel.class);
        when(model.getOptions()).thenReturn(org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build());
        List<Prompt> prompts = new ArrayList<>();
        when(model.call(any(Prompt.class))).thenAnswer(call -> {
            prompts.add(call.getArgument(0));
            return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
        });
        var tools = new FinancialAgentTools(mock(GetWalletUseCase.class),
                mock(com.tyson.digitalwallet.ddd.application.usecase.wallet.LookupRecipientUseCase.class),
                mock(com.tyson.digitalwallet.ddd.application.usecase.ai.PrepareDraftTransferUseCase.class),
                mock(com.tyson.digitalwallet.ddd.application.usecase.transaction.GetTransactionsUseCase.class));
        var local = mock(LocalKnowledgeBaseService.class);
        var knowledge = mock(VectorKnowledgePort.class);
        when(knowledge.findRelevantPolicies(any(), anyInt())).thenReturn(List.of("policy {literal}"));
        var service = new ChatFinancialAssistantUseCaseImpl(ChatClient.create(model), tools, knowledge,
                mock(GetWalletUseCase.class), mock(UserRepository.class),
                MessageWindowChatMemory.builder().maxMessages(20).build(), local);
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        assertEquals("ok", service.chat(new ChatAssistantCommand(alice, "ALICE_SECRET {raw}", "shared")).answer());
        assertEquals("ok", service.chat(new ChatAssistantCommand(bob, "BOB_QUESTION", "shared")).answer());
        assertEquals("ok", service.chat(new ChatAssistantCommand(alice, "ALICE_FOLLOWUP", "shared")).answer());
        assertEquals(3, prompts.size());
        assertFalse(prompts.get(1).getInstructions().toString().contains("ALICE_SECRET"));
        assertTrue(prompts.get(2).getInstructions().toString().contains("ALICE_SECRET {raw}"));
        assertFalse(prompts.get(2).getInstructions().toString().contains("BOB_QUESTION"));
    }
}
