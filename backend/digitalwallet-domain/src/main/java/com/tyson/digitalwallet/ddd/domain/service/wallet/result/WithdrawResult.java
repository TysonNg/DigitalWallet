package com.tyson.digitalwallet.ddd.domain.service.wallet.result;

import com.tyson.digitalwallet.ddd.domain.model.entity.Transaction;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;

public record WithdrawResult(
        Wallet wallet,
        Transaction transaction
) {}
