package com.tyson.digitalwallet.ddd.controller.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.application.usecase.ai.ChatFinancialAssistantUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.ai.SyncKnowledgeUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.ai.command.ChatAssistantCommand;
import com.tyson.digitalwallet.ddd.application.usecase.ai.response.ChatAssistantResponse;
import com.tyson.digitalwallet.ddd.application.usecase.ai.response.DraftTransferPayload;
import com.tyson.digitalwallet.ddd.controller.dto.request.ai.ChatAssistantRequestDto;
import com.tyson.digitalwallet.ddd.controller.exception.GlobalExceptionHandler;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AiAssistantControllerTest {

    private ChatFinancialAssistantUseCase chatFinancialAssistantUseCase;
    private SyncKnowledgeUseCase syncKnowledgeUseCase;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID userId = UUID.randomUUID();
    private final Principal principal = () -> userId.toString();

    @Before
    public void setup() {
        chatFinancialAssistantUseCase = mock(ChatFinancialAssistantUseCase.class);
        syncKnowledgeUseCase = mock(SyncKnowledgeUseCase.class);

        AiAssistantController controller = new AiAssistantController(
                chatFinancialAssistantUseCase,
                syncKnowledgeUseCase,
                java.util.concurrent.ForkJoinPool.commonPool()
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void chat_success_returnsAnswerAndDraftTransfer() throws Exception {
        ChatAssistantRequestDto requestDto = new ChatAssistantRequestDto(
                "Chuyển 50000 cho Nam tiền ăn",
                "conv-123"
        );

        UUID recipientUserId = UUID.randomUUID();
        UUID recipientWalletId = UUID.randomUUID();

        DraftTransferPayload draft = new DraftTransferPayload(
                recipientUserId,
                recipientWalletId,
                "Nam Nguyen",
                "0912345678",
                BigDecimal.valueOf(50000),
                "tiền ăn",
                BigDecimal.ZERO,
                true,
                "OK"
        );

        ChatAssistantResponse mockResponse = new ChatAssistantResponse(
                "Đã soạn lệnh chuyển tiền 50.000 VNĐ cho Nam.",
                draft
        );

        when(chatFinancialAssistantUseCase.chat(any(ChatAssistantCommand.class))).thenReturn(mockResponse);

        MvcResult result = mockMvc.perform(post("/ai/chat")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("Đã soạn lệnh chuyển tiền"));
        assertTrue(responseBody.contains(recipientWalletId.toString()));
        verify(chatFinancialAssistantUseCase).chat(any(ChatAssistantCommand.class));
    }

    @Test
    public void chat_blankMessage_returnsBadRequest() throws Exception {
        ChatAssistantRequestDto requestDto = new ChatAssistantRequestDto("   ", "conv-123");

        mockMvc.perform(post("/ai/chat")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void chat_oversizedInput_isRejectedBeforeCallingAi() throws Exception {
        for (var requestDto : java.util.List.of(
                new ChatAssistantRequestDto("a".repeat(4001), "conversation"),
                new ChatAssistantRequestDto("hello", "a".repeat(129)))) {
            mockMvc.perform(post("/ai/chat")
                            .principal(principal)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());
        }
        org.mockito.Mockito.verifyNoInteractions(chatFinancialAssistantUseCase);
    }

    @Test
    public void syncKnowledge_success_returnsIngestedCount() throws Exception {
        when(syncKnowledgeUseCase.sync()).thenReturn(12);

        MvcResult result = mockMvc.perform(post("/ai/knowledge/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("12"));
        assertTrue(responseBody.contains("SUCCESS"));
        verify(syncKnowledgeUseCase).sync();
    }

    @Test
    public void streamChat_returnsSseEmitter() throws Exception {
        ChatAssistantRequestDto requestDto = new ChatAssistantRequestDto("Phí nạp tiền?", "conv-456");

        ChatAssistantResponse mockResponse = new ChatAssistantResponse("Phí nạp Stripe là 2.5%", null);
        when(chatFinancialAssistantUseCase.chat(any(ChatAssistantCommand.class))).thenReturn(mockResponse);

        MvcResult result = mockMvc.perform(post("/ai/chat/stream")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(request().asyncStarted())
                .andReturn();

        assertTrue(result.getRequest().isAsyncStarted());
    }
}
