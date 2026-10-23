package com.tyson.digitalwallet.ddd.domain.service.ledger.impl;

import com.tyson.digitalwallet.ddd.domain.model.entity.Account;
import com.tyson.digitalwallet.ddd.domain.model.entity.JournalEntry;
import com.tyson.digitalwallet.ddd.domain.model.vo.TrialBalance;
import com.tyson.digitalwallet.ddd.domain.repository.AccountRepository;
import com.tyson.digitalwallet.ddd.domain.repository.LedgerRepository;
import com.tyson.digitalwallet.ddd.domain.service.ledger.LedgerService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class LedgerServiceImpl implements LedgerService {

    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;
    private final com.tyson.digitalwallet.ddd.domain.repository.WithdrawalRepository
            withdrawalRepository;

    public static final String SYS_STRIPE_CLEARING = "SYS_STRIPE_CLEARING";
    public static final String SYS_PAYOS_CLEARING = "SYS_PAYOS_CLEARING";
    public static final String SYS_BANK_VAULT = "SYS_BANK_VAULT";
    public static final String SYS_FEE_REVENUE = "SYS_FEE_REVENUE";
    public static final String SYS_GATEWAY_EXPENSE = "SYS_GATEWAY_EXPENSE";

    public LedgerServiceImpl(
            AccountRepository accountRepository, LedgerRepository ledgerRepository) {
        this(accountRepository, ledgerRepository, null);
    }

    public LedgerServiceImpl(
            AccountRepository accountRepository,
            LedgerRepository ledgerRepository,
            com.tyson.digitalwallet.ddd.domain.repository.WithdrawalRepository
                    withdrawalRepository) {
        this.withdrawalRepository = withdrawalRepository;
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    public Account getOrCreateWalletAccount(UUID walletId, String currency) {
        return accountRepository
                .findByWalletId(walletId)
                .orElseGet(
                        () -> {
                            Account newAccount = Account.createWalletAccount(walletId, currency);
                            return accountRepository.save(newAccount);
                        });
    }

    @Override
    public JournalEntry recordDeposit(
            UUID transactionId,
            UUID paymentOrderId,
            UUID walletId,
            BigDecimal amount,
            String gatewayCode,
            BigDecimal fee,
            String description) {
        Account userAccount = getOrCreateWalletAccount(walletId, "VND");

        String gatewayAccountCode;
        if ("STRIPE".equalsIgnoreCase(gatewayCode)) {
            gatewayAccountCode = SYS_STRIPE_CLEARING;
        } else if ("PAYOS".equalsIgnoreCase(gatewayCode)) {
            gatewayAccountCode = SYS_PAYOS_CLEARING;
        } else {
            gatewayAccountCode = SYS_BANK_VAULT;
        }

        Account gatewayAccount =
                accountRepository
                        .findByCode(gatewayAccountCode)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Không tìm thấy tài khoản cổng: "
                                                        + gatewayAccountCode));

        BigDecimal feeAmount =
                (fee != null && fee.compareTo(BigDecimal.ZERO) > 0) ? fee : BigDecimal.ZERO;
        BigDecimal netAmount = amount.subtract(feeAmount);

        JournalEntry journal =
                new JournalEntry(
                        UUID.randomUUID(),
                        transactionId,
                        paymentOrderId,
                        (description != null) ? description : "Nạp tiền vào ví #" + walletId,
                        LocalDateTime.now());

        // 1. Ghi Nợ (Debit) tài sản cổng thực nhận
        journal.addDebit(gatewayAccount.getId(), netAmount);

        // 2. Ghi Nợ (Debit) chi phí xử lý cổng (nếu có)
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            Account expenseAccount =
                    accountRepository
                            .findByCode(SYS_GATEWAY_EXPENSE)
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Không tìm thấy tài khoản chi phí: "
                                                            + SYS_GATEWAY_EXPENSE));
            journal.addDebit(expenseAccount.getId(), feeAmount);
        }

        // 3. Ghi Có (Credit) tăng nghĩa vụ nợ của ví User
        journal.addCredit(userAccount.getId(), amount);

        // Kiểm tra Invariant Zero-Sum
        journal.validate();

        return ledgerRepository.saveJournal(journal);
    }

    @Override
    public JournalEntry recordWithdraw(
            UUID transactionId,
            UUID walletId,
            BigDecimal amount,
            BigDecimal fee,
            String description) {
        Account userAccount = getOrCreateWalletAccount(walletId, "VND");
        Account bankVault =
                accountRepository
                        .findByCode(SYS_BANK_VAULT)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Không tìm thấy tài khoản quỹ ngân hàng: "
                                                        + SYS_BANK_VAULT));

        BigDecimal feeAmount =
                (fee != null && fee.compareTo(BigDecimal.ZERO) > 0) ? fee : BigDecimal.ZERO;
        BigDecimal netPayout = amount.subtract(feeAmount);

        JournalEntry journal =
                new JournalEntry(
                        UUID.randomUUID(),
                        transactionId,
                        null,
                        (description != null) ? description : "Rút tiền từ ví #" + walletId,
                        LocalDateTime.now());

        // 1. Ghi Nợ (Debit) giảm nghĩa vụ nợ của ví User
        journal.addDebit(userAccount.getId(), amount);

        // 2. Ghi Có (Credit) giảm tiền thực tế trong tài khoản ngân hàng cty
        journal.addCredit(bankVault.getId(), netPayout);

        // 3. Ghi Có (Credit) tăng doanh thu phí của cty (nếu có phí rút)
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            Account feeAccount =
                    accountRepository
                            .findByCode(SYS_FEE_REVENUE)
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Không tìm thấy tài khoản doanh thu phí: "
                                                            + SYS_FEE_REVENUE));
            journal.addCredit(feeAccount.getId(), feeAmount);
        }

        // Kiểm tra Invariant Zero-Sum
        journal.validate();

        return ledgerRepository.saveJournal(journal);
    }

    @Override
    public JournalEntry recordWithdrawReturn(
            UUID transactionId, UUID walletId, BigDecimal amount, String description) {
        Account user = getOrCreateWalletAccount(walletId, "VND");
        Account bank = accountRepository.findByCode(SYS_BANK_VAULT).orElseThrow();
        JournalEntry journal =
                new JournalEntry(
                        UUID.randomUUID(), transactionId, null, description, LocalDateTime.now());
        journal.addDebit(bank.getId(), amount);
        journal.addCredit(user.getId(), amount);
        journal.validate();
        return ledgerRepository.saveJournal(journal);
    }

    @Override
    public JournalEntry recordTransfer(
            UUID transactionId,
            UUID senderWalletId,
            UUID receiverWalletId,
            BigDecimal amount,
            BigDecimal fee,
            String description) {
        Account senderAccount = getOrCreateWalletAccount(senderWalletId, "VND");
        Account receiverAccount = getOrCreateWalletAccount(receiverWalletId, "VND");

        BigDecimal feeAmount =
                (fee != null && fee.compareTo(BigDecimal.ZERO) > 0) ? fee : BigDecimal.ZERO;
        BigDecimal totalSenderDeduction = amount.add(feeAmount);

        JournalEntry journal =
                new JournalEntry(
                        UUID.randomUUID(),
                        transactionId,
                        null,
                        (description != null) ? description : "Chuyển tiền giữa 2 ví",
                        LocalDateTime.now());

        // 1. Ghi Nợ (Debit) ví người gửi: Giảm số dư (tiền chuyển + phí)
        journal.addDebit(senderAccount.getId(), totalSenderDeduction);

        // 2. Ghi Có (Credit) ví người nhận: Tăng số dư
        journal.addCredit(receiverAccount.getId(), amount);

        // 3. Ghi Có (Credit) doanh thu phí công ty (nếu có phí)
        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            Account feeAccount =
                    accountRepository
                            .findByCode(SYS_FEE_REVENUE)
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Không tìm thấy tài khoản doanh thu phí: "
                                                            + SYS_FEE_REVENUE));
            journal.addCredit(feeAccount.getId(), feeAmount);
        }

        // Kiểm tra Invariant Zero-Sum
        journal.validate();

        return ledgerRepository.saveJournal(journal);
    }

    @Override
    public TrialBalance getTrialBalance() {
        return ledgerRepository.getTrialBalance();
    }

    @Override
    public boolean reconcileWallet(UUID walletId, BigDecimal currentBalance) {
        Account account = getOrCreateWalletAccount(walletId, "VND");
        BigDecimal ledgerBalance = ledgerRepository.calculateAccountBalance(account.getId());
        BigDecimal targetBalance = (currentBalance != null) ? currentBalance : BigDecimal.ZERO;
        // Wallet.balance is available balance; held withdrawals remain a ledger liability until
        // settlement.
        BigDecimal held =
                withdrawalRepository == null
                        ? BigDecimal.ZERO
                        : withdrawalRepository.sumHeldAmount(walletId);
        return ledgerBalance.compareTo(targetBalance.add(held)) == 0;
    }
}
