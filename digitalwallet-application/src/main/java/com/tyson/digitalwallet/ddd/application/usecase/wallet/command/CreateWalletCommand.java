package com.tyson.digitalwallet.ddd.application.usecase.wallet.command;

import java.util.UUID;

public record CreateWalletCommand(
        UUID userId,
        String currency
) {}
