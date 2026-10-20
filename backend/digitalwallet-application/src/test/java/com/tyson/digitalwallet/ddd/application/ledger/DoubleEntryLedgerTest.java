package com.tyson.digitalwallet.ddd.application.ledger;

import com.tyson.digitalwallet.ddd.domain.exception.UnbalancedLedgerException;
import com.tyson.digitalwallet.ddd.domain.model.entity.Account;
import com.tyson.digitalwallet.ddd.domain.model.entity.JournalEntry;
import com.tyson.digitalwallet.ddd.domain.model.enums.AccountType;
import com.tyson.digitalwallet.ddd.domain.repository.AccountRepository;
import com.tyson.digitalwallet.ddd.domain.repository.LedgerRepository;
import com.tyson.digitalwallet.ddd.domain.service.ledger.impl.LedgerServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DoubleEntryLedgerTest {

    private AccountRepository accountRepository;
    private LedgerRepository ledgerRepository;
    private LedgerServiceImpl ledgerService;

    private final UUID stripeAccountId = UUID.randomUUID();
    private final UUID expenseAccountId = UUID.randomUUID();
    private final UUID bankVaultAccountId = UUID.randomUUID();
    private final UUID feeRevenueAccountId = UUID.randomUUID();

    @Before
    public void setUp() {
        accountRepository = mock(AccountRepository.class);
        ledgerRepository = mock(LedgerRepository.class);
        ledgerService = new LedgerServiceImpl(accountRepository, ledgerRepository);

        Account stripeAccount = new Account(stripeAccountId, LedgerServiceImpl.SYS_STRIPE_CLEARING, "Stripe", AccountType.ASSET, "VND", null, LocalDateTime.now());
        Account expenseAccount = new Account(expenseAccountId, LedgerServiceImpl.SYS_GATEWAY_EXPENSE, "Expense", AccountType.EXPENSE, "VND", null, LocalDateTime.now());
        Account bankVaultAccount = new Account(bankVaultAccountId, LedgerServiceImpl.SYS_BANK_VAULT, "Bank Vault", AccountType.ASSET, "VND", null, LocalDateTime.now());
        Account feeRevenueAccount = new Account(feeRevenueAccountId, LedgerServiceImpl.SYS_FEE_REVENUE, "Fee Revenue", AccountType.REVENUE, "VND", null, LocalDateTime.now());

        when(accountRepository.findByCode(LedgerServiceImpl.SYS_STRIPE_CLEARING)).thenReturn(Optional.of(stripeAccount));
        when(accountRepository.findByCode(LedgerServiceImpl.SYS_GATEWAY_EXPENSE)).thenReturn(Optional.of(expenseAccount));
        when(accountRepository.findByCode(LedgerServiceImpl.SYS_BANK_VAULT)).thenReturn(Optional.of(bankVaultAccount));
        when(accountRepository.findByCode(LedgerServiceImpl.SYS_FEE_REVENUE)).thenReturn(Optional.of(feeRevenueAccount));

        when(ledgerRepository.saveJournal(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void testJournalEntry_Balanced_Success() {
        JournalEntry journal = new JournalEntry(UUID.randomUUID(), null, null, "Test balanced", LocalDateTime.now());
        UUID accA = UUID.randomUUID();
        UUID accB = UUID.randomUUID();

        journal.addDebit(accA, new BigDecimal("100000"));
        journal.addCredit(accB, new BigDecimal("100000"));

        // Không ném exception
        journal.validate();
        assertEquals(2, journal.getEntries().size());
    }

    @Test(expected = UnbalancedLedgerException.class)
    public void testJournalEntry_Unbalanced_ThrowsException() {
        JournalEntry journal = new JournalEntry(UUID.randomUUID(), null, null, "Test unbalanced", LocalDateTime.now());
        UUID accA = UUID.randomUUID();
        UUID accB = UUID.randomUUID();

        journal.addDebit(accA, new BigDecimal("100000"));
        journal.addCredit(accB, new BigDecimal("90000")); // Lệch 10k!

        journal.validate(); // Sẽ ném UnbalancedLedgerException
    }

    @Test(expected = UnbalancedLedgerException.class)
    public void testJournalEntry_LessThanTwoEntries_ThrowsException() {
        JournalEntry journal = new JournalEntry(UUID.randomUUID(), null, null, "Test single entry", LocalDateTime.now());
        journal.addDebit(UUID.randomUUID(), new BigDecimal("100000"));

        journal.validate();
    }

    @Test
    public void testRecordDeposit_ViaStripe_ZeroSumBalance() {
        UUID walletId = UUID.randomUUID();
        UUID walletAccountId = UUID.randomUUID();
        Account walletAccount = Account.createWalletAccount(walletId, "VND");
        walletAccount.setId(walletAccountId);

        when(accountRepository.findByWalletId(walletId)).thenReturn(Optional.of(walletAccount));

        BigDecimal depositAmount = new BigDecimal("500000");
        BigDecimal stripeFee = new BigDecimal("15000");

        JournalEntry result = ledgerService.recordDeposit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                walletId,
                depositAmount,
                "STRIPE",
                stripeFee,
                "Nạp tiền thẻ quốc tế"
        );

        assertNotNull(result);
        assertEquals(3, result.getEntries().size());

        BigDecimal totalDebit = result.getEntries().stream()
                .map(e -> e.getDebitAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = result.getEntries().stream()
                .map(e -> e.getCreditAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("500000"), totalDebit);
        assertEquals(new BigDecimal("500000"), totalCredit);
        assertEquals(totalDebit, totalCredit);
    }

    @Test
    public void testRecordTransfer_WithFee_ZeroSumBalance() {
        UUID senderWalletId = UUID.randomUUID();
        UUID receiverWalletId = UUID.randomUUID();

        Account senderAccount = Account.createWalletAccount(senderWalletId, "VND");
        Account receiverAccount = Account.createWalletAccount(receiverWalletId, "VND");

        when(accountRepository.findByWalletId(senderWalletId)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByWalletId(receiverWalletId)).thenReturn(Optional.of(receiverAccount));

        BigDecimal transferAmount = new BigDecimal("100000");
        BigDecimal fee = new BigDecimal("2000");

        JournalEntry result = ledgerService.recordTransfer(
                UUID.randomUUID(),
                senderWalletId,
                receiverWalletId,
                transferAmount,
                fee,
                "Chuyển tiền mua hàng"
        );

        assertNotNull(result);
        assertEquals(3, result.getEntries().size());

        BigDecimal totalDebit = result.getEntries().stream()
                .map(e -> e.getDebitAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = result.getEntries().stream()
                .map(e -> e.getCreditAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(new BigDecimal("102000"), totalDebit);
        assertEquals(new BigDecimal("102000"), totalCredit);
        assertEquals(totalDebit, totalCredit);
    }
}
