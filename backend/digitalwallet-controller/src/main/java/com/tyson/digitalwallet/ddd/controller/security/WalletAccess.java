package com.tyson.digitalwallet.ddd.controller.security;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.GetWalletUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import java.security.Principal;
import java.util.UUID;

/** Authorization at the HTTP boundary; internal payment webhooks use domain services directly. */
public final class WalletAccess {
    private WalletAccess() {}

    public static UUID currentUser(Principal principal) {
        if (principal == null) {
            throw new SecurityException("Authentication is required");
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException ex) {
            throw new SecurityException("Invalid authenticated user");
        }
    }

    public static void requireUser(Principal principal, UUID userId) {
        if (!currentUser(principal).equals(userId)) {
            throw new SecurityException("You cannot access another user's wallet");
        }
    }

    public static void requireOwner(Principal principal, WalletResponse wallet) {
        requireUser(principal, wallet.userId());
    }

    public static void requireOwner(Principal principal, UUID walletId, GetWalletUseCase wallets) {
        requireOwner(principal, wallets.getWalletById(walletId));
    }

    public static boolean owns(UUID userId, UUID walletId, GetWalletUseCase wallets) {
        return walletId != null && userId.equals(wallets.getWalletById(walletId).userId());
    }
}