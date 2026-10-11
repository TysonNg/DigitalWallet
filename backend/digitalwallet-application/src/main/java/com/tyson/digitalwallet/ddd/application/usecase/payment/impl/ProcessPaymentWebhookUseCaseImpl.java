package com.tyson.digitalwallet.ddd.application.usecase.payment.impl;

import com.tyson.digitalwallet.ddd.application.usecase.payment.ProcessPaymentWebhookUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.payment.command.ProcessPaymentWebhookCommand;
import com.tyson.digitalwallet.ddd.application.usecase.payment.resolver.PaymentGatewayResolver;
import com.tyson.digitalwallet.ddd.domain.event.MoneyDepositEvent;
import com.tyson.digitalwallet.ddd.domain.model.entity.PaymentOrder;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;
import com.tyson.digitalwallet.ddd.domain.repository.PaymentOrderRepository;
import com.tyson.digitalwallet.ddd.domain.service.payment.PaymentService;
import com.tyson.digitalwallet.ddd.domain.service.payment.port.PaymentGatewayPort;
import com.tyson.digitalwallet.ddd.domain.service.payment.result.PaymentWebhookResult;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.DepositResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ProcessPaymentWebhookUseCaseImpl implements ProcessPaymentWebhookUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentWebhookUseCaseImpl.class);

    private final PaymentGatewayResolver gatewayResolver;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentService paymentService;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;

    public ProcessPaymentWebhookUseCaseImpl(
        PaymentGatewayResolver gatewayResolver,
        PaymentOrderRepository paymentOrderRepository,
        PaymentService paymentService,
        WalletService walletService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.gatewayResolver = gatewayResolver;
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentService = paymentService;
        this.walletService = walletService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void processWebhook(ProcessPaymentWebhookCommand command) {
        PaymentGatewayPort gateway = gatewayResolver.resolve(command.gateway());
        PaymentWebhookResult result = gateway.verifyWebhook(command.rawPayload(), command.headers());

        if (!result.isValid()) {
            log.warn("Invalid webhook signature received for gateway: {}. Reason: {}", command.gateway(), result.message());
            return;
        }

        String gatewayOrderId = result.gatewayOrderId();
        if (gatewayOrderId == null || gatewayOrderId.isBlank()) {
            log.warn("Webhook does not contain gatewayOrderId. Gateway: {}", command.gateway());
            return;
        }

        // Khóa bi quan (Pessimistic Lock) dòng PaymentOrder để chống xử lý đồng thời
        PaymentOrder order = paymentOrderRepository.findByGatewayOrderIdWithLock(gatewayOrderId)
                .orElse(null);

        if (order == null) {
            log.warn("Payment order not found for gatewayOrderId: {} (gateway: {})", gatewayOrderId, command.gateway());
            return;
        }

        // Idempotency: Nếu đơn không còn ở trạng thái PENDING thì bỏ qua ngay lập tức
        if (!order.isPending()) {
            log.info("Payment order {} is already processed (current status={}). Skipping webhook.", order.getId(), order.getStatus());
            return;
        }

        if (result.status() == PaymentOrderStatus.SUCCESS) {
            log.info("Processing successful payment for order {}, amount: {}", order.getId(), order.getAmount());

            // 1. Ủy thác nạp tiền vào ví cho WalletService (Domain)
            DepositResult depositResult = walletService.deposit(order.getWalletId(), order.getAmount());

            // 2. Cập nhật trạng thái đơn hàng thành công và gán transaction_id thông qua PaymentService (Domain)
            paymentService.completeOrder(order.getId(), depositResult.transaction().getId());

            // 3. Bắn Spring Domain Event (TransactionNotificationListener sẽ gửi qua WebSocket STOMP)
            eventPublisher.publishEvent(new MoneyDepositEvent(
                    depositResult.transaction().getId(),
                    depositResult.wallet().getUserId(),
                    order.getAmount(),
                    depositResult.wallet().getBalance(),
                    Instant.now()
            ));

            log.info("Payment order {} successfully fulfilled. Transaction ID: {}", order.getId(), depositResult.transaction().getId());
        } else if (result.status() == PaymentOrderStatus.FAILED) {
            log.warn("Payment order {} marked as FAILED by gateway.", order.getId());
            paymentService.failOrder(order.getId());
        } else if (result.status() == PaymentOrderStatus.CANCELLED) {
            log.info("Payment order {} was CANCELLED by user/gateway.", order.getId());
            paymentService.cancelOrder(order.getId());
        }
    }
}
