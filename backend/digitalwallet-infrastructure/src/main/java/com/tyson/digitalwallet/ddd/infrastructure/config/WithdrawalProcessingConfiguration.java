package com.tyson.digitalwallet.ddd.infrastructure.config;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.WithdrawalProcessingPolicy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
public class WithdrawalProcessingConfiguration {
    @Bean
    public WithdrawalProcessingPolicy withdrawalProcessingPolicy(
            @Value("${withdrawal.processing-mode:scheduler}") String mode) {
        return new WithdrawalProcessingPolicy(mode);
    }
}
