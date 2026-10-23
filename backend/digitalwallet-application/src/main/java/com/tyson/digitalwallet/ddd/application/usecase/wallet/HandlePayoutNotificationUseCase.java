package com.tyson.digitalwallet.ddd.application.usecase.wallet;

public interface HandlePayoutNotificationUseCase {
    void webhook(String provider, String body, String signature);
}
