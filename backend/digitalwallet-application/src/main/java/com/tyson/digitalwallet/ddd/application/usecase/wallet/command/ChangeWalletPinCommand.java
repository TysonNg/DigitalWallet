package com.tyson.digitalwallet.ddd.application.usecase.wallet.command;

import java.util.UUID;

public record ChangeWalletPinCommand(
        UUID userId,
        UUID walletId,
        String oldPin,
        String newPin
) {}
