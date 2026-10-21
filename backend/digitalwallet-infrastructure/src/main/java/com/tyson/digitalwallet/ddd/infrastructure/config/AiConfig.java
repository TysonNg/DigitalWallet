package com.tyson.digitalwallet.ddd.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder
                .defaultSystem("""
                        Bạn là trợ lý tài chính AI thông minh của hệ thống Ví điện tử Digital Wallet.
                        Nhiệm vụ của bạn là hỗ trợ người dùng tra cứu chính sách phí, hạn mức giao dịch, giải đáp thắc mắc và hỗ trợ điều phối giao dịch.
                        Quy tắc quan trọng:
                        1. Luôn trả lời thân thiện, ngắn gọn, chuẩn xác bằng Tiếng Việt.
                        2. Tuyệt đối không tự ý thực hiện giao dịch trừ tiền trực tiếp. AI chỉ tạo lệnh nháp (Draft Transaction) và yêu cầu người dùng xác nhận bằng mã PIN/OTP trên giao diện.
                        3. Nếu không chắc chắn về một thông tin chính sách, hãy trả lời trung thực và hướng dẫn người dùng liên hệ CSKH.
                        """)
                .build();
    }
}
