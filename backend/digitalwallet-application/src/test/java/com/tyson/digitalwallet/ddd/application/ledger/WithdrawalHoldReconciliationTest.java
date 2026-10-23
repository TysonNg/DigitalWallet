package com.tyson.digitalwallet.ddd.application.ledger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.tyson.digitalwallet.ddd.domain.model.entity.Account;
import com.tyson.digitalwallet.ddd.domain.repository.*;
import com.tyson.digitalwallet.ddd.domain.service.ledger.impl.LedgerServiceImpl;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

public class WithdrawalHoldReconciliationTest {
    @Test
    public void ledgerBalanceIncludesHeldFundsExcludedFromAvailableBalance() {
        UUID wallet = UUID.randomUUID();
        var accounts = mock(AccountRepository.class);
        var ledger = mock(LedgerRepository.class);
        var withdrawals = mock(WithdrawalRepository.class);
        var account = Account.createWalletAccount(wallet, "VND");
        when(accounts.findByWalletId(wallet)).thenReturn(Optional.of(account));
        when(ledger.calculateAccountBalance(account.getId())).thenReturn(new BigDecimal("1000000"));
        when(withdrawals.sumHeldAmount(wallet)).thenReturn(new BigDecimal("200000"));
        var service = new LedgerServiceImpl(accounts, ledger, withdrawals);
        assertTrue(service.reconcileWallet(wallet, new BigDecimal("800000")));
        assertFalse(service.reconcileWallet(wallet, new BigDecimal("700000")));
    }
}
