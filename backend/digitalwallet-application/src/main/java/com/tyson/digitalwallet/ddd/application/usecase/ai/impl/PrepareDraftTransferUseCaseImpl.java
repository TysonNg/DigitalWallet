package com.tyson.digitalwallet.ddd.application.usecase.ai.impl;

import com.tyson.digitalwallet.ddd.application.usecase.ai.PrepareDraftTransferUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.ai.response.DraftTransferPayload;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.GetWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.LookupRecipientUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.RecipientLookupResponse;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PrepareDraftTransferUseCaseImpl implements PrepareDraftTransferUseCase {

    private static final BigDecimal MIN_TRANSFER_AMOUNT = BigDecimal.valueOf(10000);
    private static final BigDecimal TRANSFER_FEE = BigDecimal.ZERO;

    private final LookupRecipientUseCase lookupRecipientUseCase;
    private final GetWalletUseCase getWalletUseCase;

    public PrepareDraftTransferUseCaseImpl(LookupRecipientUseCase lookupRecipientUseCase,
                                          GetWalletUseCase getWalletUseCase) {
        this.lookupRecipientUseCase = lookupRecipientUseCase;
        this.getWalletUseCase = getWalletUseCase;
    }

    @Override
    public DraftTransferPayload prepareDraft(UUID currentUserId, String recipientQuery, BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(MIN_TRANSFER_AMOUNT) < 0) {
            return new DraftTransferPayload(
                    null, null, null, recipientQuery,
                    amount != null ? amount : BigDecimal.ZERO,
                    note, TRANSFER_FEE, false,
                    "Số tiền chuyển tối thiểu là 10.000 VNĐ."
            );
        }

        RecipientLookupResponse recipient;
        try {
            recipient = lookupRecipientUseCase.lookup(currentUserId, recipientQuery);
        } catch (Exception e) {
            return new DraftTransferPayload(
                    null, null, null, recipientQuery,
                    amount, note, TRANSFER_FEE, false,
                    "Không tìm thấy người nhận phù hợp với từ khóa: " + recipientQuery
            );
        }

        if (recipient == null) {
            return new DraftTransferPayload(
                    null, null, null, recipientQuery,
                    amount, note, TRANSFER_FEE, false,
                    "Không tìm thấy người nhận: " + recipientQuery
            );
        }

        WalletResponse senderWallet;
        try {
            senderWallet = getWalletUseCase.getWalletByUserId(currentUserId);
        } catch (Exception e) {
            return new DraftTransferPayload(
                    recipient.userId(), recipient.walletId(), recipient.fullName(),
                    recipient.phoneNumber() != null ? recipient.phoneNumber() : recipient.email(),
                    amount, note, TRANSFER_FEE, false,
                    "Không thể lấy thông tin ví người gửi."
            );
        }

        if (senderWallet.status() != WalletStatus.ACTIVE) {
            return new DraftTransferPayload(
                    recipient.userId(), recipient.walletId(), recipient.fullName(),
                    recipient.phoneNumber() != null ? recipient.phoneNumber() : recipient.email(),
                    amount, note, TRANSFER_FEE, false,
                    "Ví của bạn đang không ở trạng thái hoạt động."
            );
        }

        if (senderWallet.balance().compareTo(amount) < 0) {
            return new DraftTransferPayload(
                    recipient.userId(), recipient.walletId(), recipient.fullName(),
                    recipient.phoneNumber() != null ? recipient.phoneNumber() : recipient.email(),
                    amount, note, TRANSFER_FEE, false,
                    String.format("Số dư ví không đủ (Hiện có: %,d VNĐ, Cần chuyển: %,d VNĐ).",
                            senderWallet.balance().longValue(), amount.longValue())
            );
        }

        return new DraftTransferPayload(
                recipient.userId(),
                recipient.walletId(),
                recipient.fullName(),
                recipient.phoneNumber() != null ? recipient.phoneNumber() : recipient.email(),
                amount,
                (note == null || note.isBlank()) ? "Chuyển tiền qua Trợ lý AI" : note,
                TRANSFER_FEE,
                true,
                "Đã tạo lệnh chuyển tiền nháp thành công. Vui lòng kiểm tra và xác nhận bằng mã PIN."
        );
    }
}
