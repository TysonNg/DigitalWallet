package com.tyson.digitalwallet.ddd.application.usecase.admin.command;

import java.util.UUID;

public record ToggleWalletFreezeCommand(UUID walletId) {
}
