package com.tyson.digitalwallet.ddd.application.usecase.payment.resolver;

import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import com.tyson.digitalwallet.ddd.domain.service.payment.port.PaymentGatewayPort;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentGatewayResolver {

    private final Map<PaymentGatewayType, PaymentGatewayPort> gatewayMap = new EnumMap<>(PaymentGatewayType.class);

    public PaymentGatewayResolver(List<PaymentGatewayPort> gatewayPorts) {
        for (PaymentGatewayPort port : gatewayPorts) {
            gatewayMap.put(port.getGatewayType(), port);
        }
    }

    public PaymentGatewayPort resolve(PaymentGatewayType type) {
        PaymentGatewayPort port = gatewayMap.get(type);
        if (port == null) {
            throw new IllegalArgumentException("Unsupported payment gateway type: " + type);
        }
        return port;
    }
}
