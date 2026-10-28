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
    private ChatFinancialAssistantUseCaseImpl streamingService(ChatModel model) {
        when(model.getOptions()).thenReturn(org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build());
        var tools = new FinancialAgentTools(mock(GetWalletUseCase.class),
                mock(com.tyson.digitalwallet.ddd.application.usecase.wallet.LookupRecipientUseCase.class),
                mock(com.tyson.digitalwallet.ddd.application.usecase.ai.PrepareDraftTransferUseCase.class),
                mock(com.tyson.digitalwallet.ddd.application.usecase.transaction.GetTransactionsUseCase.class));
        var knowledge = mock(VectorKnowledgePort.class);
        when(knowledge.findRelevantPolicies(any(), anyInt())).thenReturn(List.of());
        return new ChatFinancialAssistantUseCaseImpl(ChatClient.create(model), tools, knowledge,
                mock(GetWalletUseCase.class), mock(UserRepository.class),
                MessageWindowChatMemory.builder().maxMessages(20).build(), mock(LocalKnowledgeBaseService.class));
    }

    private ChatResponse chunk(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    public void deliversFirstFragmentBeforeModelCompletes() throws Exception {
        var model = mock(ChatModel.class);
        var source = reactor.core.publisher.Sinks.many().unicast().<ChatResponse>onBackpressureBuffer();
        when(model.stream(any(Prompt.class))).thenReturn(source.asFlux());
        when(model.call(any(Prompt.class))).thenReturn(chunk("blocking answer"));
        var service = streamingService(model);
        var received = new java.util.concurrent.CountDownLatch(1);
        var fragments = new java.util.concurrent.CopyOnWriteArrayList<String>();
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var result = executor.submit(() -> service.stream(
                    new ChatAssistantCommand(UUID.randomUUID(), "hello", "stream"), text -> {
                        fragments.add(text);
                        received.countDown();
                    }));
            source.tryEmitNext(chunk("Xin "));
            assertTrue("First fragment must arrive before upstream completion", received.await(5, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(List.of("Xin "), fragments);
            assertFalse("Response must still be streaming", result.isDone());
            source.tryEmitNext(chunk("chào"));
            source.tryEmitComplete();
            assertEquals("Xin chào", result.get(5, java.util.concurrent.TimeUnit.SECONDS).answer());
            assertEquals(List.of("Xin ", "chào"), fragments);
            verify(model, never()).call(any(Prompt.class));
        } finally {
            source.tryEmitComplete();
            executor.shutdownNow();
        }
    }

    @Test
    public void upstreamFailureAfterContentDoesNotAppendFallbackAnswer() {
        var model = mock(ChatModel.class);
        when(model.stream(any(Prompt.class))).thenReturn(reactor.core.publisher.Flux.concat(
                reactor.core.publisher.Flux.just(chunk("partial")),
                reactor.core.publisher.Flux.error(new IllegalStateException("provider disconnected"))));
        when(model.call(any(Prompt.class))).thenReturn(chunk("blocking answer"));
        var service = streamingService(model);
        var fragments = new ArrayList<String>();
        assertThrows(RuntimeException.class, () -> service.stream(
                new ChatAssistantCommand(UUID.randomUUID(), "hello", "stream"), fragments::add));
        assertEquals(List.of("partial"), fragments);
    }

    @Test
    public void downstreamFailureCancelsModelSubscription() {
        var model = mock(ChatModel.class);
        var cancelled = new java.util.concurrent.atomic.AtomicBoolean();
        when(model.stream(any(Prompt.class))).thenReturn(reactor.core.publisher.Flux.concat(
                reactor.core.publisher.Flux.just(chunk("first")), reactor.core.publisher.Flux.never())
                .doOnCancel(() -> cancelled.set(true)));
        when(model.call(any(Prompt.class))).thenReturn(chunk("blocking answer"));
        var service = streamingService(model);
        assertThrows(RuntimeException.class, () -> service.stream(
                new ChatAssistantCommand(UUID.randomUUID(), "hello", "stream"), text -> {
                    throw new IllegalStateException("client disconnected");
                }));
        assertTrue("Closing a failed response must cancel upstream", cancelled.get());
    }
    @Test
    public void interruptBeforeFirstContentCancelsUpstreamWithoutFallback() throws Exception {
        var model = mock(ChatModel.class);
        var subscribed = new java.util.concurrent.CountDownLatch(1);
        var cancelled = new java.util.concurrent.CountDownLatch(1);
        when(model.stream(any(Prompt.class))).thenReturn(reactor.core.publisher.Flux.<ChatResponse>never()
                .doOnSubscribe(subscription -> subscribed.countDown())
                .doOnCancel(cancelled::countDown));
        var service = streamingService(model);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var result = executor.submit(() -> service.stream(
                    new ChatAssistantCommand(UUID.randomUUID(), "hello", "stream"), text -> fail("Cancelled request emitted fallback")));
            assertTrue(subscribed.await(5, java.util.concurrent.TimeUnit.SECONDS));
            result.cancel(true);
            assertTrue("Interruption must cancel model subscription", cancelled.await(5, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void streamedToolCallingReturnsDraftAndPreservesConversationMemory() {
        var model = mock(ChatModel.class);
        when(model.getOptions()).thenReturn(org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build());
        var prepare = mock(com.tyson.digitalwallet.ddd.application.usecase.ai.PrepareDraftTransferUseCase.class);
        var owner = UUID.randomUUID();
        var draft = new com.tyson.digitalwallet.ddd.application.usecase.ai.response.DraftTransferPayload(
                UUID.randomUUID(), UUID.randomUUID(), "Nam", "nam@example.com",
                new java.math.BigDecimal("100000"), "tien an", java.math.BigDecimal.ZERO, true, "OK");
        when(prepare.prepareDraft(owner, "Nam", new java.math.BigDecimal("100000"), "tien an")).thenReturn(draft);
        var tools = new FinancialAgentTools(mock(GetWalletUseCase.class),
                mock(com.tyson.digitalwallet.ddd.application.usecase.wallet.LookupRecipientUseCase.class), prepare,
                mock(com.tyson.digitalwallet.ddd.application.usecase.transaction.GetTransactionsUseCase.class));
        var prompts = new ArrayList<Prompt>();
        when(model.stream(any(Prompt.class))).thenAnswer(call -> {
            Prompt prompt = call.getArgument(0);
            prompts.add(prompt);
            if (prompts.size() == 1) {
                var toolCall = new AssistantMessage.ToolCall("call-1", "function", "prepareDraftTransfer",
                        "{\"recipientQuery\":\"Nam\",\"amount\":100000,\"note\":\"tien an\"}");
                return reactor.core.publisher.Flux.just(new ChatResponse(List.of(new Generation(
                        AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build()))));
            }
            return reactor.core.publisher.Flux.just(chunk("Draft ready"));
        });
        var knowledge = mock(VectorKnowledgePort.class);
        when(knowledge.findRelevantPolicies(any(), anyInt())).thenReturn(List.of());
        var service = new ChatFinancialAssistantUseCaseImpl(ChatClient.create(model), tools, knowledge,
                mock(GetWalletUseCase.class), mock(UserRepository.class),
                MessageWindowChatMemory.builder().maxMessages(20).build(), mock(LocalKnowledgeBaseService.class));
        var fragments = new ArrayList<String>();
        var result = service.stream(new ChatAssistantCommand(owner, "TRANSFER_REQUEST", "shared"), fragments::add);
        assertEquals(draft, result.draftTransfer());
        assertEquals("Draft ready", String.join("", fragments));
        service.stream(new ChatAssistantCommand(owner, "FOLLOW_UP", "shared"), text -> {});
        assertTrue(prompts.get(prompts.size() - 1).getInstructions().toString().contains("TRANSFER_REQUEST"));
        verify(prepare).prepareDraft(owner, "Nam", new java.math.BigDecimal("100000"), "tien an");
    }
}