package com.tyson.digitalwallet.ddd.application.usecase.payment;

import com.tyson.digitalwallet.ddd.application.usecase.payment.command.ProcessPaymentWebhookCommand;

public interface ProcessPaymentWebhookUseCase {
    void processWebhook(ProcessPaymentWebhookCommand command);
}
