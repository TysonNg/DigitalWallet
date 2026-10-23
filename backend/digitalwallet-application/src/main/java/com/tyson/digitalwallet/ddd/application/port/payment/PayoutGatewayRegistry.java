package com.tyson.digitalwallet.ddd.application.port.payment;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PayoutGatewayRegistry {
    private final Map<String, PayoutGateway> gateways;

    public PayoutGatewayRegistry(List<PayoutGateway> gateways) {
        Map<String, PayoutGateway> map = new HashMap<>();
        for (var gateway : gateways)
            if (map.put(gateway.provider(), gateway) != null)
                throw new IllegalStateException("Duplicate payout provider");
        this.gateways = Map.copyOf(map);
    }

    public PayoutGateway get(String provider) {
        var gateway = gateways.get(provider);
        if (gateway == null)
            throw new IllegalArgumentException("Unsupported payout provider: " + provider);
        return gateway;
    }
}
