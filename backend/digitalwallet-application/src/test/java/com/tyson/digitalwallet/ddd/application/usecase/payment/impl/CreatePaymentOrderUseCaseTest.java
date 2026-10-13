package com.tyson.digitalwallet.ddd.application.usecase.payment.impl;

import com.tyson.digitalwallet.ddd.application.exception.WalletNotFoundException;
import com.tyson.digitalwallet.ddd.application.usecase.payment.command.CreatePaymentOrderCommand;
import com.tyson.digitalwallet.ddd.application.usecase.payment.resolver.PaymentGatewayResolver;
import com.tyson.digitalwallet.ddd.application.usecase.payment.response.PaymentOrderResponse;
import com.tyson.digitalwallet.ddd.domain.model.entity.PaymentOrder;
import com.tyson.digitalwallet.ddd.domain.model.entity.Wallet;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;
import com.tyson.digitalwallet.ddd.domain.model.enums.WalletStatus;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.domain.service.payment.PaymentService;
import com.tyson.digitalwallet.ddd.domain.service.payment.port.PaymentGatewayPort;
import com.tyson.digitalwallet.ddd.domain.service.payment.result.PaymentLinkResult;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CreatePaymentOrderUseCaseTest {

    private final WalletRepository walletRepository = mock(WalletRepository.class);
    private final PaymentService paymentService = mock(PaymentService.class);
    private final PaymentGatewayPort payOsGateway = mock(PaymentGatewayPort.class);
    private final PaymentGatewayPort stripeGateway = mock(PaymentGatewayPort.class);

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID walletId = UUID.randomUUID();
    private final Wallet wallet = new Wallet(walletId, userId, BigDecimal.ZERO, "VND", WalletStatus.ACTIVE, null, null);

    private CreatePaymentOrderUseCaseImpl useCase;

    @Before
    public void setup() {
        when(payOsGateway.getGatewayType()).thenReturn(PaymentGatewayType.PAYOS);
        when(stripeGateway.getGatewayType()).thenReturn(PaymentGatewayType.STRIPE);

        PaymentGatewayResolver resolver = new PaymentGatewayResolver(List.of(payOsGateway, stripeGateway));
        useCase = new CreatePaymentOrderUseCaseImpl(walletRepository, paymentService, resolver);

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
    }

    @Test
    public void createOrderWithPayOsGeneratesCheckoutLinkAndUpdatesOrder() {
        BigDecimal amount = new BigDecimal("500000.00");
        PaymentOrder initialized = PaymentOrder.create(walletId, userId, amount, PaymentGatewayType.PAYOS);
        PaymentOrder finalized = PaymentOrder.create(walletId, userId, amount, PaymentGatewayType.PAYOS);
        finalized.updateCheckoutInfo("payos_order_123", "https://checkout.payos.vn/123");

        when(paymentService.initializeOrder(walletId, userId, amount, PaymentGatewayType.PAYOS))
                .thenReturn(initialized);
        when(payOsGateway.createPaymentLink(any()))
                .thenReturn(new PaymentLinkResult("payos_order_123", "https://checkout.payos.vn/123"));
        when(paymentService.updateCheckoutInfo(initialized.getId(), "payos_order_123", "https://checkout.payos.vn/123"))
                .thenReturn(finalized);

        CreatePaymentOrderCommand command = new CreatePaymentOrderCommand(
                walletId, userId, amount, PaymentGatewayType.PAYOS,
                "https://example.com/return", "https://example.com/cancel"
        );

        PaymentOrderResponse response = useCase.createPaymentOrder(command);

        assertNotNull(response);
        assertEquals(PaymentOrderStatus.PENDING, response.status());
        assertEquals("payos_order_123", response.gatewayOrderId());
        assertEquals("https://checkout.payos.vn/123", response.checkoutUrl());
        verify(paymentService).initializeOrder(walletId, userId, amount, PaymentGatewayType.PAYOS);
        verify(payOsGateway).createPaymentLink(any());
        verify(paymentService).updateCheckoutInfo(initialized.getId(), "payos_order_123", "https://checkout.payos.vn/123");
    }

    @Test
    public void createOrderWithStripeGeneratesCheckoutLinkAndUpdatesOrder() {
        BigDecimal amount = new BigDecimal("1000000.00");
        PaymentOrder initialized = PaymentOrder.create(walletId, userId, amount, PaymentGatewayType.STRIPE);
        PaymentOrder finalized = PaymentOrder.create(walletId, userId, amount, PaymentGatewayType.STRIPE);
        finalized.updateCheckoutInfo("cs_test_abc", "https://checkout.stripe.com/pay/cs_test_abc");

        when(paymentService.initializeOrder(walletId, userId, amount, PaymentGatewayType.STRIPE))
                .thenReturn(initialized);
        when(stripeGateway.createPaymentLink(any()))
                .thenReturn(new PaymentLinkResult("cs_test_abc", "https://checkout.stripe.com/pay/cs_test_abc"));
        when(paymentService.updateCheckoutInfo(initialized.getId(), "cs_test_abc", "https://checkout.stripe.com/pay/cs_test_abc"))
                .thenReturn(finalized);

        CreatePaymentOrderCommand command = new CreatePaymentOrderCommand(
                walletId, userId, amount, PaymentGatewayType.STRIPE,
                "https://example.com/return", "https://example.com/cancel"
        );

        PaymentOrderResponse response = useCase.createPaymentOrder(command);

        assertNotNull(response);
        assertEquals("cs_test_abc", response.gatewayOrderId());
        assertEquals("https://checkout.stripe.com/pay/cs_test_abc", response.checkoutUrl());
        verify(stripeGateway).createPaymentLink(any());
    }

    @Test
    public void resolvesWalletByUserIdWhenWalletIdIsNull() {
        BigDecimal amount = new BigDecimal("200000.00");
        PaymentOrder initialized = PaymentOrder.create(walletId, userId, amount, PaymentGatewayType.PAYOS);
        PaymentOrder finalized = PaymentOrder.create(walletId, userId, amount, PaymentGatewayType.PAYOS);
        finalized.updateCheckoutInfo("payos_order_456", "https://checkout.payos.vn/456");

        when(paymentService.initializeOrder(walletId, userId, amount, PaymentGatewayType.PAYOS))
                .thenReturn(initialized);
        when(payOsGateway.createPaymentLink(any()))
                .thenReturn(new PaymentLinkResult("payos_order_456", "https://checkout.payos.vn/456"));
        when(paymentService.updateCheckoutInfo(initialized.getId(), "payos_order_456", "https://checkout.payos.vn/456"))
                .thenReturn(finalized);

        CreatePaymentOrderCommand command = new CreatePaymentOrderCommand(
                null, userId, amount, PaymentGatewayType.PAYOS,
                "https://example.com/return", "https://example.com/cancel"
        );

        PaymentOrderResponse response = useCase.createPaymentOrder(command);

        assertNotNull(response);
        verify(walletRepository).findByUserId(userId);
        verify(paymentService).initializeOrder(walletId, userId, amount, PaymentGatewayType.PAYOS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroOrNegativeAmount() {
        CreatePaymentOrderCommand command = new CreatePaymentOrderCommand(
                walletId, userId, BigDecimal.ZERO, PaymentGatewayType.PAYOS,
                "https://example.com/return", "https://example.com/cancel"
        );
        useCase.createPaymentOrder(command);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullAmount() {
        CreatePaymentOrderCommand command = new CreatePaymentOrderCommand(
                walletId, userId, null, PaymentGatewayType.PAYOS,
                "https://example.com/return", "https://example.com/cancel"
        );
        useCase.createPaymentOrder(command);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsWhenWalletDoesNotBelongToUser() {
        CreatePaymentOrderCommand command = new CreatePaymentOrderCommand(
                walletId, otherUserId, new BigDecimal("100000.00"), PaymentGatewayType.PAYOS,
                "https://example.com/return", "https://example.com/cancel"
        );
        useCase.createPaymentOrder(command);
    }

    @Test(expected = WalletNotFoundException.class)
    public void throwsWhenWalletNotFoundById() {
        UUID unknownWallet = UUID.randomUUID();
        when(walletRepository.findById(unknownWallet)).thenReturn(Optional.empty());

        CreatePaymentOrderCommand command = new CreatePaymentOrderCommand(
                unknownWallet, userId, new BigDecimal("100000.00"), PaymentGatewayType.PAYOS,
                "https://example.com/return", "https://example.com/cancel"
        );
        useCase.createPaymentOrder(command);
    }

    @Test(expected = WalletNotFoundException.class)
    public void throwsWhenWalletNotFoundByUserId() {
        UUID unknownUser = UUID.randomUUID();
        when(walletRepository.findByUserId(unknownUser)).thenReturn(Optional.empty());

        CreatePaymentOrderCommand command = new CreatePaymentOrderCommand(
                null, unknownUser, new BigDecimal("100000.00"), PaymentGatewayType.PAYOS,
                "https://example.com/return", "https://example.com/cancel"
        );
        useCase.createPaymentOrder(command);
    }
}
