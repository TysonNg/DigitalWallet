package com.tyson.digitalwallet.ddd.application.usecase.wallet.command;

import java.util.UUID;

public record SetupWalletPinCommand(
        UUID userId,
        UUID walletId,
        String pin
) {}
