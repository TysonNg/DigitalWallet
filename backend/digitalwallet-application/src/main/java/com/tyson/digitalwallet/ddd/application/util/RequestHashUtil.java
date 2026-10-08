package com.tyson.digitalwallet.ddd.application.util;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public final class RequestHashUtil {

    private RequestHashUtil() {}

    public static String computeTransferHash(
            UUID senderWalletId,
            UUID receiverWalletId,
            BigDecimal amount,
            String description
    ) {
        String raw = String.format(
                "sender:%s|receiver:%s|amount:%s|desc:%s",
                senderWalletId,
                receiverWalletId,
                amount != null ? amount.stripTrailingZeros().toPlainString() : "0",
                description != null ? description.trim() : ""
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
