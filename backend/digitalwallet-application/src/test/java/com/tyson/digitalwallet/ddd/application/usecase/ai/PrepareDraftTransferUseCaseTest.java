package com.tyson.digitalwallet.ddd.application.usecase.ai;

import com.tyson.digitalwallet.ddd.application.usecase.ai.impl.PrepareDraftTransferUseCaseImpl;
import com.tyson.digitalwallet.ddd.application.usecase.ai.response.DraftTransferPayload;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.GetWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.LookupRecipientUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.RecipientLookupResponse;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PrepareDraftTransferUseCaseTest {

    private LookupRecipientUseCase lookupRecipientUseCase;
    private GetWalletUseCase getWalletUseCase;
    private PrepareDraftTransferUseCase prepareDraftTransferUseCase;

    private final UUID senderUserId = UUID.randomUUID();
    private final UUID recipientUserId = UUID.randomUUID();
    private final UUID recipientWalletId = UUID.randomUUID();

    @Before
    public void setUp() {
        lookupRecipientUseCase = Mockito.mock(LookupRecipientUseCase.class);
        getWalletUseCase = Mockito.mock(GetWalletUseCase.class);
        prepareDraftTransferUseCase = new PrepareDraftTransferUseCaseImpl(lookupRecipientUseCase, getWalletUseCase);
    }

    @Test
    public void prepareDraft_WhenAmountBelowMinimum_ReturnsCannotProceed() {
        DraftTransferPayload payload = prepareDraftTransferUseCase.prepareDraft(
                senderUserId, "Nam", BigDecimal.valueOf(5000), "Tien an trua"
        );

        assertNotNull(payload);
        assertFalse(payload.canProceed());
        assertTrue(payload.message().contains("tối thiểu là 10.000 VNĐ"));
    }

    @Test
    public void prepareDraft_WhenRecipientNotFound_ReturnsCannotProceed() {
        when(lookupRecipientUseCase.lookup(eq(senderUserId), eq("Unknown")))
                .thenReturn(null);

        DraftTransferPayload payload = prepareDraftTransferUseCase.prepareDraft(
                senderUserId, "Unknown", BigDecimal.valueOf(50000), "Tien an trua"
        );

        assertNotNull(payload);
        assertFalse(payload.canProceed());
        assertTrue(payload.message().contains("Không tìm thấy người nhận"));
    }

    @Test
    public void prepareDraft_WhenSenderBalanceInsufficient_ReturnsCannotProceed() {
        RecipientLookupResponse recipient = new RecipientLookupResponse(
                recipientUserId, recipientWalletId, "Nguyen Van Nam", "0987654321", "nam@test.com", "VND"
        );
        when(lookupRecipientUseCase.lookup(eq(senderUserId), eq("Nam"))).thenReturn(recipient);

        WalletResponse senderWallet = new WalletResponse(
                UUID.randomUUID(), senderUserId, BigDecimal.valueOf(20000), "VND",
                WalletStatus.ACTIVE, true, LocalDateTime.now(), LocalDateTime.now()
        );
        when(getWalletUseCase.getWalletByUserId(senderUserId)).thenReturn(senderWallet);

        DraftTransferPayload payload = prepareDraftTransferUseCase.prepareDraft(
                senderUserId, "Nam", BigDecimal.valueOf(50000), "Tien an trua"
        );

        assertNotNull(payload);
        assertFalse(payload.canProceed());
        assertTrue(payload.message().contains("Số dư ví không đủ"));
    }

    @Test
    public void prepareDraft_WhenValid_ReturnsSuccessDraft() {
        RecipientLookupResponse recipient = new RecipientLookupResponse(
                recipientUserId, recipientWalletId, "Nguyen Van Nam", "0987654321", "nam@test.com", "VND"
        );
        when(lookupRecipientUseCase.lookup(eq(senderUserId), eq("Nam"))).thenReturn(recipient);

        WalletResponse senderWallet = new WalletResponse(
                UUID.randomUUID(), senderUserId, BigDecimal.valueOf(200000), "VND",
                WalletStatus.ACTIVE, true, LocalDateTime.now(), LocalDateTime.now()
        );
        when(getWalletUseCase.getWalletByUserId(senderUserId)).thenReturn(senderWallet);

        DraftTransferPayload payload = prepareDraftTransferUseCase.prepareDraft(
                senderUserId, "Nam", BigDecimal.valueOf(50000), "Tien an trua"
        );

        assertNotNull(payload);
        assertTrue(payload.canProceed());
        assertEquals("Nguyen Van Nam", payload.recipientName());
        assertEquals(BigDecimal.valueOf(50000), payload.amount());
        assertEquals("Tien an trua", payload.note());
        assertEquals(BigDecimal.ZERO, payload.fee());
    }
}
