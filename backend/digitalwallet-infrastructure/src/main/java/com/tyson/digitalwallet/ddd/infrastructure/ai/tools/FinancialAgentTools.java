package com.tyson.digitalwallet.ddd.infrastructure.ai.tools;

import com.tyson.digitalwallet.ddd.application.usecase.ai.PrepareDraftTransferUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.ai.response.DraftTransferPayload;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.GetWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.LookupRecipientUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.RecipientLookupResponse;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.infrastructure.ai.context.AiContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class FinancialAgentTools {

    private static final Logger log = LoggerFactory.getLogger(FinancialAgentTools.class);

    private final GetWalletUseCase getWalletUseCase;
    private final LookupRecipientUseCase lookupRecipientUseCase;
    private final PrepareDraftTransferUseCase prepareDraftTransferUseCase;

    public FinancialAgentTools(GetWalletUseCase getWalletUseCase,
                              LookupRecipientUseCase lookupRecipientUseCase,
                              PrepareDraftTransferUseCase prepareDraftTransferUseCase) {
        this.getWalletUseCase = getWalletUseCase;
        this.lookupRecipientUseCase = lookupRecipientUseCase;
        this.prepareDraftTransferUseCase = prepareDraftTransferUseCase;
    }

    @Tool(description = "Kiểm tra số dư khả dụng và trạng thái ví của người dùng hiện tại")
    public String checkCurrentBalance() {
        UUID currentUserId = AiContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            return "Không xác định được danh tính người dùng hiện tại.";
        }
        try {
            WalletResponse wallet = getWalletUseCase.getWalletByUserId(currentUserId);
            return String.format("Số dư ví khả dụng hiện tại: %,d %s. Trạng thái ví: %s.",
                    wallet.balance().longValue(), wallet.currency(), wallet.status());
        } catch (Exception e) {
            log.error("Failed to check balance in AI tool: {}", e.getMessage());
            return "Không thể kiểm tra số dư vào lúc này: " + e.getMessage();
        }
    }

    @Tool(description = "Tìm kiếm thông tin người nhận theo tên, số điện thoại hoặc email để chuẩn bị chuyển tiền")
    public String lookupRecipient(
            @ToolParam(description = "Tên, số điện thoại hoặc địa chỉ email của người nhận") String keyword) {
        UUID currentUserId = AiContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            return "Không xác định được danh tính người dùng hiện tại.";
        }
        try {
            RecipientLookupResponse recipient = lookupRecipientUseCase.lookup(currentUserId, keyword);
            if (recipient == null) {
                return "Không tìm thấy người nhận với từ khóa: " + keyword;
            }
            return String.format("Tìm thấy thông tin người nhận: Họ tên: %s | SĐT: %s | Email: %s | Mã ví: %s",
                    recipient.fullName(),
                    recipient.phoneNumber() != null ? recipient.phoneNumber() : "N/A",
                    recipient.email() != null ? recipient.email() : "N/A",
                    recipient.walletId());
        } catch (Exception e) {
            log.warn("Lookup recipient failed in AI tool: {}", e.getMessage());
            return "Không tìm thấy người nhận phù hợp: " + e.getMessage();
        }
    }

    @Tool(description = "Soạn lệnh chuyển tiền nháp (Draft Transfer) để hiển thị thẻ xác nhận trên màn hình. AI không được tự ý trừ tiền.")
    public String prepareDraftTransfer(
            @ToolParam(description = "Tên, số điện thoại hoặc email người nhận") String recipientQuery,
            @ToolParam(description = "Số tiền cần chuyển (VNĐ)") double amount,
            @ToolParam(description = "Lời nhắn / nội dung chuyển tiền") String note) {
        UUID currentUserId = AiContextHolder.getCurrentUserId();
        if (currentUserId == null) {
            return "Không xác định được danh tính người dùng.";
        }

        try {
            DraftTransferPayload payload = prepareDraftTransferUseCase.prepareDraft(
                    currentUserId,
                    recipientQuery,
                    BigDecimal.valueOf(amount),
                    note
            );

            if (payload.canProceed()) {
                AiContextHolder.setDraftTransfer(payload);
                return String.format("Đã tạo lệnh chuyển tiền nháp thành công: Chuyển %,d VNĐ cho %s (%s). " +
                                "Lời nhắn: '%s'. Phí: 0 VNĐ. Thẻ xác nhận giao dịch đã được hiển thị trên giao diện người dùng. " +
                                "Vui lòng yêu cầu người dùng bấm 'Xác nhận & Nhập mã PIN' trên thẻ để hoàn tất.",
                        payload.amount().longValue(), payload.recipientName(), payload.recipientPhoneOrEmail(), payload.note());
            } else {
                return "Không thể tạo lệnh chuyển tiền: " + payload.message();
            }
        } catch (Exception e) {
            log.error("Failed to prepare draft transfer in AI tool: {}", e.getMessage());
            return "Đã xảy ra lỗi khi tạo lệnh chuyển tiền nháp: " + e.getMessage();
        }
    }
}
