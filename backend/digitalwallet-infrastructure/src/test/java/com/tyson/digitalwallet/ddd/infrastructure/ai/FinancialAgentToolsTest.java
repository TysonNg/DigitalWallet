package com.tyson.digitalwallet.ddd.infrastructure.ai;

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
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.infrastructure.ai.tools.FinancialAgentTools;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FinancialAgentToolsTest {

    private GetWalletUseCase getWalletUseCase;
    private LookupRecipientUseCase lookupRecipientUseCase;
    private PrepareDraftTransferUseCase prepareDraftTransferUseCase;
    private GetTransactionsUseCase getTransactionsUseCase;
    private FinancialAgentTools tools;

    private final UUID userId = UUID.randomUUID();
    private final UUID walletId = UUID.randomUUID();

    @Before
    public void setup() {
        getWalletUseCase = mock(GetWalletUseCase.class);
        lookupRecipientUseCase = mock(LookupRecipientUseCase.class);
        prepareDraftTransferUseCase = mock(PrepareDraftTransferUseCase.class);
        getTransactionsUseCase = mock(GetTransactionsUseCase.class);

        tools = new FinancialAgentTools(
                getWalletUseCase,
                lookupRecipientUseCase,
                prepareDraftTransferUseCase,
                getTransactionsUseCase
        );
    }


    @Test
    public void checkCurrentBalance_onAnotherThread_keepsAuthenticatedUser() throws Exception {
        tools = tools.forUser(userId);
        WalletResponse wallet = new WalletResponse(walletId, userId, BigDecimal.valueOf(2500000),
                "VND", WalletStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        when(getWalletUseCase.getWalletByUserId(userId)).thenReturn(wallet);

        String result = java.util.concurrent.CompletableFuture.supplyAsync(tools::checkCurrentBalance).get();

        assertTrue(result, result.contains("2,500,000"));
    }

    @Test
    public void requestTools_doNotShareDraftsAndRejectFractionalVnd() throws Exception {
        tools = tools.forUser(userId);
        DraftTransferPayload draft = new DraftTransferPayload(UUID.randomUUID(), walletId, "Nam", "nam@example.com",
                BigDecimal.valueOf(50000), "lunch", BigDecimal.ZERO, true, "OK");
        when(prepareDraftTransferUseCase.prepareDraft(userId, "Nam", BigDecimal.valueOf(50000), "lunch"))
                .thenReturn(draft);
        java.util.concurrent.CompletableFuture.runAsync(() ->
                tools.prepareDraftTransfer("Nam", BigDecimal.valueOf(50000), "lunch")).get();
        org.junit.Assert.assertEquals(draft, tools.getDraftTransfer());
        org.junit.Assert.assertNull(tools.forUser(UUID.randomUUID()).getDraftTransfer());
        tools.prepareDraftTransfer("Nam", new BigDecimal("50000.5"), "lunch");
        org.junit.Assert.assertNull(tools.getDraftTransfer());
    }

    @Test
    public void checkCurrentBalance_withoutUserContext_returnsError() {
        String result = tools.checkCurrentBalance();
        assertTrue(result.contains("Không xác định được danh tính"));
    }

    @Test
    public void checkCurrentBalance_withUserContext_returnsBalance() {
        tools = tools.forUser(userId);
        WalletResponse wallet = new WalletResponse(
                walletId,
                userId,
                BigDecimal.valueOf(2500000),
                "VND",
                WalletStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(getWalletUseCase.getWalletByUserId(userId)).thenReturn(wallet);

        String result = tools.checkCurrentBalance();
        assertTrue(result.contains("2,500,000"));
        assertTrue(result.contains("ACTIVE"));
    }

    @Test
    public void lookupRecipient_found_returnsRecipientInfo() {
        tools = tools.forUser(userId);
        RecipientLookupResponse recipient = new RecipientLookupResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Alice Nguyen",
                "0988776655",
                "alice@example.com",
                "VND"
        );
        when(lookupRecipientUseCase.lookup(userId, "Alice")).thenReturn(recipient);

        String result = tools.lookupRecipient("Alice");
        assertTrue(result.contains("Alice Nguyen"));
        assertTrue(result.contains("0988776655"));
    }

    @Test
    public void prepareDraftTransfer_valid_setsContextAndReturnsPrompt() {
        tools = tools.forUser(userId);
        DraftTransferPayload draft = new DraftTransferPayload(
                UUID.randomUUID(),
                walletId,
                "Bob Tran",
                "bob@example.com",
                BigDecimal.valueOf(100000),
                "cafe",
                BigDecimal.ZERO,
                true,
                "OK"
        );
        when(prepareDraftTransferUseCase.prepareDraft(eq(userId), eq("Bob"), eq(BigDecimal.valueOf(100000.0)), eq("cafe")))
                .thenReturn(draft);

        String result = tools.prepareDraftTransfer("Bob", BigDecimal.valueOf(100000.0), "cafe");
        assertTrue(result.contains("Đã tạo lệnh chuyển tiền nháp thành công"));
        assertTrue(result.contains("Bob Tran"));
        assertNotNull(tools.getDraftTransfer());
    }

    @Test
    public void queryRecentTransactions_returnsFormattedHistory() {
        tools = tools.forUser(userId);
        WalletResponse wallet = new WalletResponse(
                walletId,
                userId,
                BigDecimal.valueOf(500000),
                "VND",
                WalletStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(getWalletUseCase.getWalletByUserId(userId)).thenReturn(wallet);

        TransactionResponse tx1 = new TransactionResponse(
                UUID.randomUUID(),
                TransactionType.DEPOSIT,
                BigDecimal.valueOf(200000),
                BigDecimal.valueOf(300000),
                BigDecimal.valueOf(500000),
                TransactionStatus.SUCCESS,
                "Nạp tiền PayOS",
                null,
                walletId,
                LocalDateTime.now().minusHours(1)
        );

        TransactionResponse tx2 = new TransactionResponse(
                UUID.randomUUID(),
                TransactionType.TRANSFER_OUT,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(350000),
                BigDecimal.valueOf(300000),
                TransactionStatus.SUCCESS,
                "Chuyển tiền ăn trưa",
                walletId,
                UUID.randomUUID(),
                LocalDateTime.now().minusDays(1)
        );

        when(getTransactionsUseCase.getTransactionsByWalletId(walletId)).thenReturn(List.of(tx1, tx2));

        String result = tools.queryRecentTransactions(5);
        assertTrue(result.contains("Nạp tiền"));
        assertTrue(result.contains("+200,000 VNĐ"));
        assertTrue(result.contains("Chuyển tiền đi"));
        assertTrue(result.contains("-50,000 VNĐ"));
    }

    @Test
    public void getSpendingSummary_calculatesTotals() {
        tools = tools.forUser(userId);
        WalletResponse wallet = new WalletResponse(
                walletId,
                userId,
                BigDecimal.valueOf(1000000),
                "VND",
                WalletStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(getWalletUseCase.getWalletByUserId(userId)).thenReturn(wallet);

        TransactionResponse deposit = new TransactionResponse(
                UUID.randomUUID(),
                TransactionType.DEPOSIT,
                BigDecimal.valueOf(500000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(500000),
                TransactionStatus.SUCCESS,
                "Nạp tiền",
                null,
                walletId,
                LocalDateTime.now()
        );

        TransactionResponse transferOut = new TransactionResponse(
                UUID.randomUUID(),
                TransactionType.TRANSFER_OUT,
                BigDecimal.valueOf(150000),
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(350000),
                TransactionStatus.SUCCESS,
                "Chuyển tiền",
                walletId,
                UUID.randomUUID(),
                LocalDateTime.now()
        );

        when(getTransactionsUseCase.getTransactionsByWalletId(walletId)).thenReturn(List.of(deposit, transferOut));

        String summary = tools.getSpendingSummary();
        assertTrue(summary.contains("+500,000 VND"));
        assertTrue(summary.contains("-150,000 VND"));
        assertTrue(summary.contains("1,000,000 VND"));
    }
}
