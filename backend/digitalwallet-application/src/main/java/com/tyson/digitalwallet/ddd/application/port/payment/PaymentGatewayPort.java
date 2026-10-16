package com.tyson.digitalwallet.ddd.application.port.payment;

import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;
import com.tyson.digitalwallet.ddd.application.port.payment.command.CreatePaymentLinkCommand;
import com.tyson.digitalwallet.ddd.application.port.payment.result.PaymentLinkResult;
import com.tyson.digitalwallet.ddd.application.port.payment.result.PaymentWebhookResult;

import java.util.Map;

public interface PaymentGatewayPort {
    PaymentGatewayType getGatewayType();

    PaymentLinkResult createPaymentLink(CreatePaymentLinkCommand command);

    PaymentWebhookResult verifyWebhook(String rawPayload, Map<String, String> headers);

    PaymentOrderStatus queryOrderStatus(String gatewayOrderId);
}
