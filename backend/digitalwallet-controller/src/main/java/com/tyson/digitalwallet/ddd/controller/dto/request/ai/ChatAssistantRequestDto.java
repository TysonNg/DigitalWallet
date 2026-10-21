package com.tyson.digitalwallet.ddd.controller.dto.request.ai;

import jakarta.validation.constraints.NotBlank;

public record ChatAssistantRequestDto(
        @NotBlank(message = "Nội dung tin nhắn không được để trống")
        String message,
        String conversationId
) {
}
