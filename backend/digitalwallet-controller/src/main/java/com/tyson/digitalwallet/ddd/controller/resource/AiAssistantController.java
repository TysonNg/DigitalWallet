package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.ai.ChatFinancialAssistantUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.ai.SyncKnowledgeUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.ai.command.ChatAssistantCommand;
import com.tyson.digitalwallet.ddd.application.usecase.ai.response.ChatAssistantResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.ai.ChatAssistantRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.ai.ChatAssistantResponseDto;
import com.tyson.digitalwallet.ddd.controller.security.WalletAccess;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import org.springframework.beans.factory.annotation.Qualifier;

@RestController
@RequestMapping("/ai")
public class AiAssistantController {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantController.class);

    private final ChatFinancialAssistantUseCase chatFinancialAssistantUseCase;
    private final SyncKnowledgeUseCase syncKnowledgeUseCase;
    private final Executor aiChatExecutor;

    public AiAssistantController(ChatFinancialAssistantUseCase chatFinancialAssistantUseCase,
                                SyncKnowledgeUseCase syncKnowledgeUseCase,
                                @Qualifier("aiChatExecutor") Executor aiChatExecutor) {
        this.chatFinancialAssistantUseCase = chatFinancialAssistantUseCase;
        this.syncKnowledgeUseCase = syncKnowledgeUseCase;
        this.aiChatExecutor = aiChatExecutor;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatAssistantResponseDto>> chat(
            @RequestBody @Valid ChatAssistantRequestDto request,
            Principal principal) {
        UUID userId = WalletAccess.currentUser(principal);
        ChatAssistantCommand command = new ChatAssistantCommand(userId, request.message(), request.conversationId());
        ChatAssistantResponse response = chatFinancialAssistantUseCase.chat(command);
        return ApiResponse.ok("AI response generated successfully", ChatAssistantResponseDto.from(response));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestBody @Valid ChatAssistantRequestDto request,
            Principal principal) {
        UUID userId = WalletAccess.currentUser(principal);
        SseEmitter emitter = new SseEmitter(60_000L);

        FutureTask<Void> task = new FutureTask<>(() -> {
            try {
                ChatAssistantCommand command = new ChatAssistantCommand(userId, request.message(), request.conversationId());
                ChatAssistantResponse response = chatFinancialAssistantUseCase.stream(command, fragment -> {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new java.util.concurrent.CancellationException("Client disconnected");
                    }
                    try {
                        emitter.send(SseEmitter.event().name("token").data(Map.of("content", fragment)));
                    } catch (java.io.IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
                // Nếu có giao dịch nháp do Function Calling sinh ra, gửi sự kiện draft_transfer
                if (response.draftTransfer() != null) {
                    emitter.send(SseEmitter.event()
                            .name("draft_transfer")
                            .data(response.draftTransfer()));
                }

                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Error during AI chat streaming: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of("error", "Không thể xử lý yêu cầu AI. Vui lòng thử lại.")));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        }, null);
        emitter.onTimeout(() -> task.cancel(true));
        emitter.onError(error -> task.cancel(true));
        emitter.onCompletion(() -> task.cancel(true));
        try {
            aiChatExecutor.execute(task);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            task.cancel(false);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @PostMapping("/knowledge/sync")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncKnowledge() {
        int count = syncKnowledgeUseCase.sync();
        return ApiResponse.ok("Knowledge Base synced successfully", Map.of(
                "ingestedChunks", count,
                "status", "SUCCESS"
        ));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/chat/conversation/{conversationId}")
    public ResponseEntity<ApiResponse<Void>> clearConversation(
            @org.springframework.web.bind.annotation.PathVariable String conversationId,
            Principal principal) {
        UUID userId = WalletAccess.currentUser(principal);
        chatFinancialAssistantUseCase.clearConversation(userId, conversationId);
        return ApiResponse.ok("Xóa lịch sử hội thoại thành công", null);
    }
}
