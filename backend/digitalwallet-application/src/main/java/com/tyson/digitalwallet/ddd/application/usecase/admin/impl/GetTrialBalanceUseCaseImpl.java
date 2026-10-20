package com.tyson.digitalwallet.ddd.application.usecase.admin.impl;

import com.tyson.digitalwallet.ddd.application.usecase.admin.GetTrialBalanceUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.admin.response.TrialBalanceResponse;
import com.tyson.digitalwallet.ddd.domain.model.vo.TrialBalance;
import com.tyson.digitalwallet.ddd.domain.service.ledger.LedgerService;
import org.springframework.stereotype.Service;

@Service
public class GetTrialBalanceUseCaseImpl implements GetTrialBalanceUseCase {

    private final LedgerService ledgerService;

    public GetTrialBalanceUseCaseImpl(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @Override
    public TrialBalanceResponse getTrialBalance() {
        TrialBalance tb = ledgerService.getTrialBalance();
        return new TrialBalanceResponse(
                tb.totalDebit(),
                tb.totalCredit(),
                tb.balanced(),
                tb.difference(),
                tb.totalEntries()
        );
    }
}
