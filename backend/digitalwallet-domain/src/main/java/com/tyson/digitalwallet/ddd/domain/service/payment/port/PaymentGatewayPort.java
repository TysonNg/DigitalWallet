package com.tyson.digitalwallet.ddd.domain.service.payment.port;

import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import com.tyson.digitalwallet.ddd.domain.service.payment.command.CreatePaymentLinkCommand;
import com.tyson.digitalwallet.ddd.domain.service.payment.result.PaymentLinkResult;
import com.tyson.digitalwallet.ddd.domain.service.payment.result.PaymentWebhookResult;

import java.util.Map;

public interface PaymentGatewayPort {
    PaymentGatewayType getGatewayType();

    PaymentLinkResult createPaymentLink(CreatePaymentLinkCommand command);

    PaymentWebhookResult verifyWebhook(String rawPayload, Map<String, String> headers);
}
