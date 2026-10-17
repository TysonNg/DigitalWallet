package com.tyson.digitalwallet.ddd.application.usecase.payment.impl;

import com.tyson.digitalwallet.ddd.application.usecase.payment.ProcessPaymentWebhookUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.payment.command.ProcessPaymentWebhookCommand;
import com.tyson.digitalwallet.ddd.application.usecase.payment.resolver.PaymentGatewayResolver;
import com.tyson.digitalwallet.ddd.domain.event.MoneyDepositEvent;
import com.tyson.digitalwallet.ddd.domain.model.entity.PaymentOrder;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentOrderStatus;
import com.tyson.digitalwallet.ddd.domain.repository.PaymentOrderRepository;
import com.tyson.digitalwallet.ddd.domain.repository.WalletRepository;
import com.tyson.digitalwallet.ddd.application.port.lock.DistributedLockService;
import com.tyson.digitalwallet.ddd.domain.service.payment.PaymentService;
import com.tyson.digitalwallet.ddd.application.port.payment.PaymentGatewayPort;
import com.tyson.digitalwallet.ddd.application.port.payment.result.PaymentWebhookResult;
import com.tyson.digitalwallet.ddd.domain.service.wallet.WalletService;
import com.tyson.digitalwallet.ddd.domain.service.wallet.result.DepositResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class ProcessPaymentWebhookUseCaseImpl implements ProcessPaymentWebhookUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentWebhookUseCaseImpl.class);

    private final PaymentGatewayResolver gatewayResolver;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentService paymentService;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DistributedLockService lockService;
    private final TransactionTemplate transactionTemplate;

    public ProcessPaymentWebhookUseCaseImpl(
        PaymentGatewayResolver gatewayResolver,
        PaymentOrderRepository paymentOrderRepository,
        PaymentService paymentService,
        WalletService walletService,
        WalletRepository walletRepository,
        ApplicationEventPublisher eventPublisher,
        DistributedLockService lockService,
        TransactionTemplate transactionTemplate
    ) {
        this.gatewayResolver = gatewayResolver;
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentService = paymentService;
        this.walletService = walletService;
        this.walletRepository = walletRepository;
        this.eventPublisher = eventPublisher;
        this.lockService = lockService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
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

        String lockKey = "lock:payment:webhook:" + gatewayOrderId;
        lockService.executeWithLock(lockKey, 3, 10, TimeUnit.SECONDS, (Runnable) () -> {
            transactionTemplate.execute(status -> {
                doProcessWebhookInternal(result, command.gateway(), gatewayOrderId);
                return null;
            });
        });
    }

    private void doProcessWebhookInternal(PaymentWebhookResult result, PaymentGatewayType gatewayType, String gatewayOrderId) {

        // Khóa bi quan (Pessimistic Lock) dòng PaymentOrder để chống xử lý đồng thời
        PaymentOrder order = paymentOrderRepository.findByGatewayOrderIdWithLock(gatewayOrderId)
                .orElse(null);

        if (order == null) {
            log.warn("Payment order not found for gatewayOrderId: {} (gateway: {})", gatewayOrderId, gatewayType);
            return;
        }

        if (order.getGateway() != gatewayType) {
            log.warn("Payment gateway mismatch for order {}", order.getId());
            return;
        }

        // Idempotency: Nếu đơn không còn ở trạng thái PENDING thì bỏ qua ngay lập tức
        if (!order.isPending()) {
            log.info("Payment order {} is already processed (current status={}). Skipping webhook.", order.getId(), order.getStatus());
            return;
        }

        if (result.status() == PaymentOrderStatus.SUCCESS) {
            if (result.amount() == null || result.amount().signum() <= 0
                    || result.amount().compareTo(order.getAmount()) != 0) {
                log.warn("Payment amount mismatch for order {}: expected {}, received {}",
                        order.getId(), order.getAmount(), result.amount());
                return;
            }
            var wallet = walletRepository.findById(order.getWalletId())
                    .orElseThrow(() -> new IllegalStateException("Payment wallet not found: " + order.getWalletId()));
            // Both checkout integrations create VND payments; never credit another currency without conversion.
            if (!"VND".equalsIgnoreCase(result.currency())
                    || !"VND".equalsIgnoreCase(wallet.getCurrency())) {
                log.warn("Payment currency mismatch for order {}: wallet {}, received {}",
                        order.getId(), wallet.getCurrency(), result.currency());
                return;
            }
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
