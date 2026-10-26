package com.tyson.digitalwallet.ddd.application.usecase.wallet;

public record WithdrawalProcessingPolicy(String mode) {
    public WithdrawalProcessingPolicy {
        if (!"kafka".equals(mode) && !"scheduler".equals(mode))
            throw new IllegalArgumentException(
                    "withdrawal.processing-mode must be kafka or scheduler");
    }

    public boolean usesKafka() {
        return "kafka".equals(mode);
    }
}
