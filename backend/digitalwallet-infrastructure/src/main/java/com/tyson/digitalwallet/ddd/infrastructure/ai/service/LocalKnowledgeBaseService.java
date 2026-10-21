package com.tyson.digitalwallet.ddd.infrastructure.ai.service;

import com.tyson.digitalwallet.ddd.application.usecase.ai.response.DraftTransferPayload;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.infrastructure.ai.tools.FinancialAgentTools;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class LocalKnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(LocalKnowledgeBaseService.class);

    @Value("${spring.ai.openai.api-key:demo-key}")
    private String apiKey;

    private final List<PolicySection> sections = new ArrayList<>();
    private final FinancialAgentTools financialAgentTools;

    public record PolicySection(String source, String title, String content, Set<String> keywords) {}

    public LocalKnowledgeBaseService(FinancialAgentTools financialAgentTools) {
        this.financialAgentTools = financialAgentTools;
    }

    @PostConstruct
    public void init() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:knowledge-base/*.md");

            sections.clear();
            for (Resource resource : resources) {
                String filename = resource.getFilename() != null ? resource.getFilename() : "unknown.md";
                try (InputStream is = resource.getInputStream()) {
                    String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    parseMarkdownIntoSections(filename, text);
                }
            }
            log.info("LocalKnowledgeBaseService initialized with {} policy sections from knowledge-base markdown files.", sections.size());
        } catch (Exception e) {
            log.error("Failed to load local knowledge-base markdown files: {}", e.getMessage(), e);
        }
    }

    public boolean isOfflineMode() {
        return apiKey == null || apiKey.isBlank() || apiKey.trim().equalsIgnoreCase("demo-key");
    }

    private void parseMarkdownIntoSections(String source, String text) {
        // Tách theo tiêu đề ##
        String[] parts = text.split("(?=##\\s)");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("# ")) {
                continue;
            }

            String title = "Chính sách";
            int firstLineEnd = trimmed.indexOf('\n');
            if (firstLineEnd != -1) {
                title = trimmed.substring(0, firstLineEnd).replace("##", "").trim();
            }

            Set<String> keywords = extractKeywords(trimmed);
            sections.add(new PolicySection(source, title, trimmed, keywords));
        }
    }

    public List<String> searchPolicies(String query, int topK) {
        if (sections.isEmpty() || query == null || query.isBlank() || topK <= 0) {
            return Collections.emptyList();
        }

        Set<String> queryKeywords = extractKeywords(query);
        String normalizedQuery = normalize(query);
        if (!containsAny(normalizedQuery, "phi", "stripe", "payos", "vietqr", "han muc", "pin", "otp",
                "bao mat", "chuyen", "rut", "nap", "giao dich", "hoan tien", "khieu nai")) {
            return List.of();
        }

        record ScoredSection(PolicySection section, int score) {}
        List<ScoredSection> scored = new ArrayList<>();

        for (PolicySection sec : sections) {
            int score = 0;
            String normalizedTitle = normalize(sec.title());
            String normalizedContent = normalize(sec.content());

            // Ưu tiên khớp nguyên cụm hoặc từ khóa đặc thù
            if (normalizedQuery.contains("stripe") && normalizedContent.contains("stripe")) {
                score += 50;
            }
            if ((normalizedQuery.contains("payos") || normalizedQuery.contains("vietqr")) && (normalizedContent.contains("payos") || normalizedContent.contains("vietqr"))) {
                score += 50;
            }
            if (normalizedQuery.contains("han muc") && (normalizedTitle.contains("han muc") || normalizedContent.contains("han muc"))) {
                score += 40;
            }
            if ((normalizedQuery.contains("pin") || normalizedQuery.contains("otp")) && (normalizedTitle.contains("pin") || normalizedContent.contains("pin"))) {
                score += 40;
            }
            if (normalizedQuery.contains("rut tien") && normalizedContent.contains("rut tien")) {
                score += 30;
            }
            if (normalizedQuery.contains("chuyen tien") && normalizedContent.contains("chuyen tien")) {
                score += 30;
            }

            // Đếm số từ khóa trùng khớp
            for (String kw : queryKeywords) {
                if (sec.keywords().contains(kw)) {
                    score += 5;
                }
                if (normalizedTitle.contains(kw)) {
                    score += 10;
                }
            }

            if (score >= 10) {
                scored.add(new ScoredSection(sec, score));
            }
        }

        scored.sort((a, b) -> Integer.compare(b.score(), a.score()));

        return scored.stream()
                .limit(Math.max(1, topK))
                .map(s -> "[Source: " + s.section().source() + " - " + s.section().title() + "]\n" + s.section().content())
                .toList();
    }

    public String generateSmartResponse(String query, String userName, WalletResponse wallet, List<String> relevantPolicies) {
        return generateSmartResponse(query, userName, wallet, relevantPolicies, financialAgentTools);
    }

    public String generateSmartResponse(String query, String userName, WalletResponse wallet, List<String> relevantPolicies, FinancialAgentTools tools) {
        String normQuery = normalize(query);

        // 1. Phân tích ý định chuyển tiền (Transfer Intent)
        if (containsAny(normQuery, "chuyen", "gui")) {
            String draftResponse = tryHandleTransferIntent(query, userName, tools);
            if (draftResponse != null) {
                return draftResponse;
            }
            if (normQuery.matches("^(chuyen|gui)( tien| khoan)?\\s+.*")
                    && !containsAny(normQuery, "phi", "han muc", "gioi han", "toi da", "toi thieu")) {
                return "Vui lòng ghi rõ số tiền và người nhận, dùng 'nội dung' trước lời nhắn. Ví dụ: Chuyển 50k cho Nam nội dung ăn trưa.";
            }
        }

        // 2. Tra cứu số dư (Balance Intent)
        if (containsAny(normQuery, "so du", "con bao nhieu", "kiem tra vi", "xem vi", "balance")) {
            if (wallet == null) return "Không lấy được thông tin ví, vui lòng thử lại sau.";
            long balance = wallet != null ? wallet.balance().longValue() : 0;
            String currency = wallet != null ? wallet.currency() : "VND";
            String status = (wallet != null && wallet.status() != null) ? wallet.status().name() : "UNKNOWN";
            return String.format("""
                    Xin chào **%s**! 👋
                    
                    💰 **Thông tin số dư ví của bạn:**
                    - Số dư khả dụng: **%,d %s**
                    - Trạng thái ví: **%s**
                    
                    Bạn có cần tôi hỗ trợ thực hiện chuyển tiền hoặc tra cứu biểu phí không?
                    """, userName, balance, currency, status);
        }

        // 3. Tra cứu giao dịch gần đây (Recent Transactions)
        if (containsAny(normQuery, "giao dich gan day", "lich su", "lich su giao dich", "sao ke")) {
            try {
                return tools.queryRecentTransactions(5);
            } catch (Exception e) {
                log.warn("Failed to query recent transactions in fallback: {}", e.getMessage());
            }
        }

        // 9. Nếu có tài liệu RAG tương ứng, hiển thị trích dẫn chính xác
        if (!relevantPolicies.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Chào **%s**! Dưới đây là thông tin chính sách trích xuất từ tài liệu chính thức của hệ thống:\n\n", userName));
            for (String policy : relevantPolicies) {
                sb.append(policy).append("\n\n---\n\n");
            }
            sb.append("💡 *Nếu bạn cần hỗ trợ thêm thao tác nào khác (như nạp tiền, chuyển tiền), hãy nhắn cho tôi nhé!*");
            return sb.toString();
        }

        return "Chưa tìm thấy tài liệu phù hợp. Vui lòng mô tả cụ thể hơn hoặc liên hệ CSKH. Không gửi PIN hay mật khẩu trong chat.";
    }

    private String tryHandleTransferIntent(String query, String userName, FinancialAgentTools tools) {
        try {
            var transfer = TransferIntentParser.parse(query);
            if (transfer == null) return null;
            BigDecimal amount = transfer.amount();
            String recipient = transfer.recipient();
            String note = transfer.note();

            if (!recipient.isBlank()) {
                String toolResult = tools.prepareDraftTransfer(recipient, amount, note);
                DraftTransferPayload draft = tools.getDraftTransfer();

                if (draft != null && draft.canProceed()) {
                    return String.format("""
                            Chào **%s**, tôi đã giúp bạn soạn sẵn **lệnh chuyển tiền nháp**:
                            
                            - 👤 **Người nhận:** **%s** (%s)
                            - 💵 **Số tiền chuyển:** **%,d VNĐ**
                            - 📝 **Lời nhắn:** %s
                            - 💸 **Phí giao dịch:** **0 VNĐ (Miễn phí)**
                            
                            ⚠️ **Lưu ý an toàn:** Tiền trong ví chưa bị trừ. Vui lòng kiểm tra kỹ thông tin và nhấn nút **Xác nhận chuyển tiền** trên thẻ giao dịch bên dưới để nhập mã PIN hoàn tất giao dịch!
                            """, userName, draft.recipientName(), draft.recipientPhoneOrEmail(), draft.amount().longValue(), draft.note());
                } else {
                    return "⚠️ " + toolResult;
                }
            }
        } catch (IllegalArgumentException e) {
            return "Chưa xác định được số tiền. Vui lòng ghi rõ, ví dụ: Chuyển 50.000đ cho Nam nội dung ăn trưa.";
        } catch (Exception e) {
            log.warn("Transfer intent parsing skipped: {}", e.getMessage());
        }
        return null;
    }

    private boolean containsAny(String target, String... candidates) {
        for (String c : candidates) {
            if (target.contains(c)) return true;
        }
        return false;
    }

    private Set<String> extractKeywords(String text) {
        String norm = normalize(text);
        String[] words = norm.split("[\\s,;:.?!()\"'#*\\-_/]+");
        Set<String> set = new HashSet<>();
        for (String w : words) {
            if (w.length() >= 2 && !Set.of("toi", "cua", "la", "co", "cho", "va", "the", "nao", "bao", "nhieu", "trong", "voi", "duoc", "khong", "hom", "nay").contains(w)) {
                set.add(w);
            }
        }
        return set;
    }

    private String normalize(String input) {
        if (input == null) return "";
        String temp = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd');
    }
}
