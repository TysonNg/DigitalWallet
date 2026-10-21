package com.tyson.digitalwallet.ddd.application.usecase.ai;

import com.tyson.digitalwallet.ddd.application.usecase.ai.response.DraftTransferPayload;

import java.math.BigDecimal;
import java.util.UUID;

public interface PrepareDraftTransferUseCase {
    DraftTransferPayload prepareDraft(UUID currentUserId, String recipientQuery, BigDecimal amount, String note);
}
