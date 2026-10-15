package com.tyson.digitalwallet.ddd.application.usecase.payment.impl;

import com.tyson.digitalwallet.ddd.application.exception.WalletNotFoundException;
import com.tyson.digitalwallet.ddd.application.usecase.payment.CreatePaymentOrderUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.payment.command.CreatePaymentOrderCommand;
import com.tyson.digitalwallet.ddd.application.usecase.payment.resolver.PaymentGatewayResolver;
import com.tyson.digitalwallet.ddd.application.usecase.payment.response.PaymentOrderResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.PaymentOrder;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.domain.service.payment.PaymentService;
import com.tyson.digitalwallet.ddd.application.port.payment.command.CreatePaymentLinkCommand;
import com.tyson.digitalwallet.ddd.application.port.payment.PaymentGatewayPort;
import com.tyson.digitalwallet.ddd.application.port.payment.result.PaymentLinkResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CreatePaymentOrderUseCaseImpl implements CreatePaymentOrderUseCase {

    private final WalletRepository walletRepository;
    private final PaymentService paymentService;
    private final PaymentGatewayResolver gatewayResolver;

    public CreatePaymentOrderUseCaseImpl(
            WalletRepository walletRepository,
            PaymentService paymentService,
            PaymentGatewayResolver gatewayResolver
    ) {
        this.walletRepository = walletRepository;
        this.paymentService = paymentService;
        this.gatewayResolver = gatewayResolver;
    }

    @Override
    @Transactional
    public PaymentOrderResponse createPaymentOrder(CreatePaymentOrderCommand command) {
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        Wallet wallet;
        if (command.walletId() != null) {
            wallet = walletRepository.findById(command.walletId())
                    .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + command.walletId()));
        } else {
            wallet = walletRepository.findByUserId(command.userId())
                    .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + command.userId()));
        }

        if (!wallet.getUserId().equals(command.userId())) {
            throw new IllegalArgumentException("Wallet does not belong to the authenticated user");
        }

        UUID effectiveWalletId = wallet.getId();

        // 1. Ủy thác khởi tạo đơn hàng PENDING cho PaymentService (Domain)
        PaymentOrder savedOrder = paymentService.initializeOrder(
                effectiveWalletId,
                command.userId(),
                command.amount(),
                command.gateway()
        );

        // 2. Gọi Cổng thanh toán để tạo link thanh toán / VietQR
        PaymentGatewayPort gateway = gatewayResolver.resolve(command.gateway());
        CreatePaymentLinkCommand linkCmd = new CreatePaymentLinkCommand(
                savedOrder.getId(),
                command.amount(),
                "Nap vi " + savedOrder.getId().toString().substring(0, 8),
                command.returnUrl(),
                command.cancelUrl()
        );

        PaymentLinkResult linkResult = gateway.createPaymentLink(linkCmd);

        // 3. Cập nhật mã đơn của cổng và checkout URL thông qua PaymentService
        PaymentOrder finalOrder = paymentService.updateCheckoutInfo(
                savedOrder.getId(),
                linkResult.gatewayOrderId(),
                linkResult.checkoutUrl()
        );

        return PaymentOrderResponse.from(finalOrder);
    }
}
