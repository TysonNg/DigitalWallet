package com.tyson.digitalwallet.ddd.controller.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatAssistantRequestDto(
        @NotBlank(message = "Nội dung tin nhắn không được để trống")
        @Size(max = 4000, message = "Tin nhắn tối đa 4000 ký tự")
        String message,
        @Size(max = 128, message = "Mã hội thoại tối đa 128 ký tự")
        String conversationId
) {
}
