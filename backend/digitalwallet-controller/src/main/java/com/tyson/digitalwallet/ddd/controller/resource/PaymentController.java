package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.payment.CreatePaymentOrderUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.payment.ProcessPaymentWebhookUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.payment.command.ProcessPaymentWebhookCommand;
import com.tyson.digitalwallet.ddd.application.usecase.payment.response.PaymentOrderResponse;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.request.payment.CreatePaymentOrderRequestDto;
import com.tyson.digitalwallet.ddd.controller.dto.response.payment.PaymentOrderResponseDto;
import com.tyson.digitalwallet.ddd.domain.model.enums.PaymentGatewayType;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final CreatePaymentOrderUseCase createPaymentOrderUseCase;
    private final ProcessPaymentWebhookUseCase processPaymentWebhookUseCase;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public PaymentController(
            CreatePaymentOrderUseCase createPaymentOrderUseCase,
            ProcessPaymentWebhookUseCase processPaymentWebhookUseCase
    ) {
        this.createPaymentOrderUseCase = createPaymentOrderUseCase;
        this.processPaymentWebhookUseCase = processPaymentWebhookUseCase;
    }

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<PaymentOrderResponseDto>> createOrder(
            @RequestBody @Valid CreatePaymentOrderRequestDto requestDto
    ) {
        String defaultReturnUrl = frontendUrl + "/dashboard?payment=success";
        String defaultCancelUrl = frontendUrl + "/dashboard?payment=cancelled";

        PaymentOrderResponse response = createPaymentOrderUseCase.createPaymentOrder(
                requestDto.toCommand(defaultReturnUrl, defaultCancelUrl)
        );

        return ApiResponse.ok("Tạo đơn nạp tiền thành công", PaymentOrderResponseDto.from(response));
    }

    @PostMapping("/webhook/payos")
    public ResponseEntity<Map<String, Object>> handlePayOsWebhook(
            @RequestBody String rawBody,
            @RequestHeader Map<String, String> headers
    ) {
        log.info("Received PayOS webhook call");
        processPaymentWebhookUseCase.processWebhook(
                new ProcessPaymentWebhookCommand(PaymentGatewayType.PAYOS, rawBody, headers)
        );
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<Map<String, Object>> handleStripeWebhook(
            @RequestBody String rawBody,
            @RequestHeader Map<String, String> headers
    ) {
        log.info("Received Stripe webhook call");
        processPaymentWebhookUseCase.processWebhook(
                new ProcessPaymentWebhookCommand(PaymentGatewayType.STRIPE, rawBody, headers)
        );
        return ResponseEntity.ok(Map.of("received", true));
    }
}
