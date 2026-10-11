package com.tyson.digitalwallet.ddd.application.usecase.payment;

import com.tyson.digitalwallet.ddd.application.usecase.payment.command.CreatePaymentOrderCommand;
import com.tyson.digitalwallet.ddd.application.usecase.payment.response.PaymentOrderResponse;

public interface CreatePaymentOrderUseCase {
    PaymentOrderResponse createPaymentOrder(CreatePaymentOrderCommand command);
}
