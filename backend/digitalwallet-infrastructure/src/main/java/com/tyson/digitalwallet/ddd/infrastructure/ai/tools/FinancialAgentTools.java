package com.tyson.digitalwallet.ddd.infrastructure.ai.tools;

import com.tyson.digitalwallet.ddd.application.usecase.ai.PrepareDraftTransferUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.ai.response.DraftTransferPayload;
import com.tyson.digitalwallet.ddd.application.usecase.transaction.GetTransactionsUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.transaction.response.TransactionResponse;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.GetWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.LookupRecipientUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.RecipientLookupResponse;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
public class FinancialAgentTools {

    private static final Logger log = LoggerFactory.getLogger(FinancialAgentTools.class);

    private final GetWalletUseCase getWalletUseCase;
    private final LookupRecipientUseCase lookupRecipientUseCase;
    private final PrepareDraftTransferUseCase prepareDraftTransferUseCase;
    private final GetTransactionsUseCase getTransactionsUseCase;
    private final UUID currentUserId;
    private volatile DraftTransferPayload draftTransfer;

    @Autowired
    public FinancialAgentTools(GetWalletUseCase getWalletUseCase,
                              LookupRecipientUseCase lookupRecipientUseCase,
                              PrepareDraftTransferUseCase prepareDraftTransferUseCase,
                              GetTransactionsUseCase getTransactionsUseCase) {
        this(getWalletUseCase, lookupRecipientUseCase, prepareDraftTransferUseCase, getTransactionsUseCase, null);
    }

    private FinancialAgentTools(GetWalletUseCase getWalletUseCase,
                                LookupRecipientUseCase lookupRecipientUseCase,
                                PrepareDraftTransferUseCase prepareDraftTransferUseCase,
                                GetTransactionsUseCase getTransactionsUseCase, UUID currentUserId) {
        this.getWalletUseCase = getWalletUseCase;
        this.lookupRecipientUseCase = lookupRecipientUseCase;
        this.prepareDraftTransferUseCase = prepareDraftTransferUseCase;
        this.getTransactionsUseCase = getTransactionsUseCase;
        this.currentUserId = currentUserId;
    }

    /** Server creates one instance per request; identity is never a model argument. */
    public FinancialAgentTools forUser(UUID userId) {
        return new FinancialAgentTools(getWalletUseCase, lookupRecipientUseCase,
                prepareDraftTransferUseCase, getTransactionsUseCase, java.util.Objects.requireNonNull(userId));
    }

    public DraftTransferPayload getDraftTransfer() {
        return draftTransfer;
    }

    @Tool(description = "Kiểm tra số dư khả dụng và trạng thái ví của người dùng hiện tại")
    public String checkCurrentBalance() {
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
            @ToolParam(description = "Số tiền nguyên cần chuyển (VNĐ)") BigDecimal amount,
            @ToolParam(description = "Lời nhắn / nội dung chuyển tiền") String note) {
        draftTransfer = null;
        if (currentUserId == null) {
            return "Không xác định được danh tính người dùng.";
        }
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0
                || amount.compareTo(BigDecimal.valueOf(9007199254740991L)) > 0) {
            return "Số tiền phải là số nguyên VNĐ dương trong phạm vi hỗ trợ.";
        }

        try {
            DraftTransferPayload payload = prepareDraftTransferUseCase.prepareDraft(
                    currentUserId,
                    recipientQuery,
                    amount,
                    note
            );

            if (payload.canProceed()) {
                draftTransfer = payload;
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

    @Tool(description = "Tra cứu danh sách các giao dịch gần đây của người dùng (nạp tiền, rút tiền, chuyển tiền, nhận tiền)")
    public String queryRecentTransactions(
            @ToolParam(description = "Số lượng giao dịch cần lấy (mặc định 5, tối đa 10)") int limit) {
        if (currentUserId == null) {
            return "Không xác định được danh tính người dùng hiện tại.";
        }
        int effectiveLimit = (limit <= 0) ? 5 : Math.min(limit, 10);
        try {
            WalletResponse wallet = getWalletUseCase.getWalletByUserId(currentUserId);
            List<TransactionResponse> txList = getTransactionsUseCase.getTransactionsByWalletId(wallet.id());
            if (txList == null || txList.isEmpty()) {
                return "Bạn chưa có giao dịch nào gần đây.";
            }

            List<TransactionResponse> recent = txList.stream()
                    .sorted((a, b) -> {
                        if (a.createdAt() == null || b.createdAt() == null) return 0;
                        return b.createdAt().compareTo(a.createdAt());
                    })
                    .limit(effectiveLimit)
                    .toList();

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Dưới đây là %d giao dịch gần nhất của bạn:\n", recent.size()));
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int index = 1;
            for (TransactionResponse tx : recent) {
                String sign = (tx.type() == TransactionType.DEPOSIT || tx.type() == TransactionType.TRANSFER_IN) ? "+" : "-";
                String typeName = switch (tx.type()) {
                    case DEPOSIT -> "Nạp tiền";
                    case WITHDRAW -> "Rút tiền";
                    case TRANSFER_OUT -> "Chuyển tiền đi";
                    case TRANSFER_IN -> "Nhận tiền";
                };
                String timeStr = tx.createdAt() != null ? tx.createdAt().format(dtf) : "N/A";
                String desc = (tx.description() != null && !tx.description().isBlank()) ? (" - " + tx.description()) : "";
                sb.append(String.format("%d. [%s] %s: %s%,d VNĐ (%s)%s\n",
                        index++,
                        timeStr,
                        typeName,
                        sign,
                        tx.amount().longValue(),
                        tx.status() == TransactionStatus.SUCCESS ? "Thành công" : tx.status().name(),
                        desc
                ));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to query recent transactions in AI tool: {}", e.getMessage());
            return "Không thể tra cứu lịch sử giao dịch: " + e.getMessage();
        }
    }

    @Tool(description = "Phân tích và tổng kết dòng tiền thu chi (tổng nạp, tổng chi/chuyển đi) từ các giao dịch gần đây")
    public String getSpendingSummary() {
        if (currentUserId == null) {
            return "Không xác định được danh tính người dùng hiện tại.";
        }
        try {
            WalletResponse wallet = getWalletUseCase.getWalletByUserId(currentUserId);
            List<TransactionResponse> txList = getTransactionsUseCase.getTransactionsByWalletId(wallet.id());
            if (txList == null || txList.isEmpty()) {
                return "Chưa có đủ dữ liệu giao dịch để lập báo cáo thu chi.";
            }

            BigDecimal totalIn = BigDecimal.ZERO;
            BigDecimal totalOut = BigDecimal.ZERO;
            int inCount = 0;
            int outCount = 0;

            for (TransactionResponse tx : txList) {
                if (tx.status() != TransactionStatus.SUCCESS) continue;
                if (tx.type() == TransactionType.DEPOSIT || tx.type() == TransactionType.TRANSFER_IN) {
                    totalIn = totalIn.add(tx.amount());
                    inCount++;
                } else if (tx.type() == TransactionType.WITHDRAW || tx.type() == TransactionType.TRANSFER_OUT) {
                    totalOut = totalOut.add(tx.amount());
                    outCount++;
                }
            }

            return String.format("""
                    Báo cáo tổng quan dòng tiền của bạn:
                    - Tổng tiền nạp / nhận vào: +%,d %s (%d giao dịch)
                    - Tổng tiền rút / chuyển đi: -%,d %s (%d giao dịch)
                    - Số dư khả dụng hiện tại: %,d %s
                    """,
                    totalIn.longValue(), wallet.currency(), inCount,
                    totalOut.longValue(), wallet.currency(), outCount,
                    wallet.balance().longValue(), wallet.currency()
            );
        } catch (Exception e) {
            log.error("Failed to generate spending summary in AI tool: {}", e.getMessage());
            return "Không thể tổng kết dòng tiền: " + e.getMessage();
        }
    }
}
