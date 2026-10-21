package com.tyson.digitalwallet.ddd.infrastructure.ai.service;

import com.tyson.digitalwallet.ddd.application.port.out.ai.VectorKnowledgePort;
import com.tyson.digitalwallet.ddd.application.usecase.ai.ChatFinancialAssistantUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.ai.command.ChatAssistantCommand;
import com.tyson.digitalwallet.ddd.application.usecase.ai.response.ChatAssistantResponse;
import com.tyson.digitalwallet.ddd.application.usecase.ai.response.DraftTransferPayload;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.GetWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.User;
import com.tyson.digitalwallet.ddd.domain.repository.UserRepository;
import com.tyson.digitalwallet.ddd.infrastructure.ai.context.AiContextHolder;
import com.tyson.digitalwallet.ddd.infrastructure.ai.tools.FinancialAgentTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChatFinancialAssistantUseCaseImpl implements ChatFinancialAssistantUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChatFinancialAssistantUseCaseImpl.class);

    private final ChatClient chatClient;
    private final FinancialAgentTools financialAgentTools;
    private final VectorKnowledgePort vectorKnowledgePort;
    private final GetWalletUseCase getWalletUseCase;
    private final UserRepository userRepository;
    private final ChatMemory chatMemory;

    public ChatFinancialAssistantUseCaseImpl(ChatClient chatClient,
                                            FinancialAgentTools financialAgentTools,
                                            VectorKnowledgePort vectorKnowledgePort,
                                            GetWalletUseCase getWalletUseCase,
                                            UserRepository userRepository,
                                            ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.financialAgentTools = financialAgentTools;
        this.vectorKnowledgePort = vectorKnowledgePort;
        this.getWalletUseCase = getWalletUseCase;
        this.userRepository = userRepository;
        this.chatMemory = chatMemory;
    }

    @Override
    public ChatAssistantResponse chat(ChatAssistantCommand command) {
        AiContextHolder.setCurrentUserId(command.userId());
        try {
            // 1. Nạp ngữ cảnh động của người dùng hiện tại
            String userName = "Quý khách";
            Optional<User> userOpt = userRepository.findById(command.userId());
            if (userOpt.isPresent()) {
                userName = userOpt.get().getFullName();
            }

            WalletResponse wallet = null;
            try {
                wallet = getWalletUseCase.getWalletByUserId(command.userId());
            } catch (Exception e) {
                log.warn("Could not retrieve wallet for AI context: {}", e.getMessage());
            }

            // 2. Truy xuất tri thức (RAG Retrieval) từ Vector Database
            List<String> relevantPolicies = vectorKnowledgePort.findRelevantPolicies(command.message(), 3);
            String policyContext = relevantPolicies.isEmpty()
                    ? "Không có tài liệu chính sách bổ sung cho câu hỏi này."
                    : String.join("\n\n---\n\n", relevantPolicies);

            // 3. Xây dựng Augmented Prompt với Dynamic Context và Grounded Policies
            String systemPrompt = String.format("""
                    Bạn là Trợ lý tài chính thông minh chính thức của Ví điện tử Digital Wallet.
                    
                    NGỮ CẢNH NGƯỜI DÙNG HIỆN TẠI:
                    - Họ và tên: %s
                    - Số dư ví khả dụng: %,d %s
                    - Trạng thái ví: %s
                    
                    TÀI LIỆU CHÍNH SÁCH CHÍNH THỨC (RAG RETRIEVED POLICIES):
                    %s
                    
                    QUY TẮC PHẢN HỒI (FINTECH SAFETY RULES):
                    1. Luôn trả lời lịch sự, thân thiện, súc tích bằng Tiếng Việt. Định dạng Markdown rõ ràng, dễ đọc (dùng bullet points, in đậm số tiền).
                    2. Nếu câu hỏi liên quan đến phí giao dịch, hạn mức ngày/lần, quy định mã PIN/OTP: BẮT BUỘC trả lời chính xác dựa theo tài liệu chính sách ở trên.
                    3. Khi người dùng có ý định chuyển tiền (ví dụ "Chuyển 50k cho Nam"): Hãy gọi tool `prepareDraftTransfer` để soạn bản nháp. TUYỆT ĐỐI không bao giờ nói rằng tiền đã được chuyển, mà phải thông báo rằng thẻ xác nhận giao dịch đã được tạo trên màn hình để người dùng nhập mã PIN xác nhận.
                    4. Tuyệt đối không bao giờ hỏi mật khẩu hoặc mã PIN của người dùng trong tin nhắn chat.
                    """,
                    userName,
                    wallet != null ? wallet.balance().longValue() : 0,
                    wallet != null ? wallet.currency() : "VND",
                    wallet != null ? wallet.status() : "UNKNOWN",
                    policyContext
            );

            // 4. Xác định conversationId cho bộ nhớ hội thoại nhiều lượt (Multi-turn ChatMemory)
            String conversationId = (command.conversationId() != null && !command.conversationId().isBlank())
                    ? command.conversationId()
                    : command.userId().toString();

            // 5. Gọi LLM qua Spring AI ChatClient với Function Calling tools và Chat Memory
            String answer;
            try {
                answer = chatClient.prompt()
                        .system(systemPrompt)
                        .user(command.message())
                        .tools(financialAgentTools)
                        .advisors(advisorSpec -> advisorSpec
                                .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                                .param("chat_memory_conversation_id", conversationId)
                        )
                        .call()
                        .content();
            } catch (Exception e) {
                log.error("Error invoking LLM ChatClient: {}", e.getMessage(), e);
                answer = generateFallbackResponse(command.message(), userName, wallet, relevantPolicies);
            }

            // 5. Kiểm tra xem tool có sinh ra lệnh chuyển tiền nháp hay không
            DraftTransferPayload draftTransfer = AiContextHolder.getDraftTransfer();

            return new ChatAssistantResponse(answer, draftTransfer);
        } finally {
            AiContextHolder.clear();
        }
    }

    private String generateFallbackResponse(String userMessage, String userName, WalletResponse wallet, List<String> policies) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Xin chào **%s**! Hiện tại mô hình AI đang trong chế độ dự phòng ngoại tuyến.\n\n", userName));

        if (wallet != null) {
            sb.append(String.format("- Số dư ví của bạn: **%,d %s** (Trạng thái: %s)\n\n",
                    wallet.balance().longValue(), wallet.currency(), wallet.status()));
        }

        if (!policies.isEmpty()) {
            sb.append("📋 **Thông tin chính sách tìm thấy liên quan đến câu hỏi của bạn:**\n\n");
            for (String policy : policies) {
                sb.append(policy).append("\n\n---\n\n");
            }
        } else {
            sb.append("Tôi đã ghi nhận câu hỏi: *\"").append(userMessage).append("\"*. ")
              .append("Bạn có thể tra cứu thông tin biểu phí tại mục Cài đặt hoặc liên hệ CSKH để được giải đáp chi tiết nhất.");
        }
        return sb.toString();
    }
}
