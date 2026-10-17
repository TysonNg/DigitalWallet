package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.wallet.*;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.WalletResponse;
import com.tyson.digitalwallet.ddd.application.usecase.transaction.GetTransactionsUseCase;
import com.tyson.digitalwallet.ddd.application.usecase.payment.*;
import com.tyson.digitalwallet.ddd.application.usecase.payment.command.CreatePaymentOrderCommand;
import com.tyson.digitalwallet.ddd.application.usecase.payment.command.ProcessPaymentWebhookCommand;
import com.tyson.digitalwallet.ddd.application.usecase.payment.response.PaymentOrderResponse;
import com.tyson.digitalwallet.ddd.application.usecase.transaction.response.TransactionResponse;
import com.tyson.digitalwallet.ddd.application.usecase.wallet.response.RecipientLookupResponse;
import com.tyson.digitalwallet.ddd.controller.exception.GlobalExceptionHandler;
import com.tyson.digitalwallet.ddd.domain.model.enums.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

public class WalletAuthorizationTest {
    private final UUID user = UUID.randomUUID();
    private final UUID otherUser = UUID.randomUUID();
    private final UUID ownWallet = UUID.randomUUID();
    private final UUID otherWallet = UUID.randomUUID();
    private final Principal principal = () -> user.toString();
    private final GetWalletUseCase wallets = mock(GetWalletUseCase.class);
    private final CreateWalletUseCase create = mock(CreateWalletUseCase.class);
    private final ProcessPaymentWebhookUseCase webhook = mock(ProcessPaymentWebhookUseCase.class);
    private final WithdrawUseCase withdraw = mock(WithdrawUseCase.class);
    private final TransferMoneyUseCase transfer = mock(TransferMoneyUseCase.class);
    private final GetTransactionsUseCase transactions = mock(GetTransactionsUseCase.class);
    private final CreatePaymentOrderUseCase payment = mock(CreatePaymentOrderUseCase.class);
    private final LookupRecipientUseCase lookupRecipient = mock(LookupRecipientUseCase.class);
    private MockMvc mvc;

    @Before public void setup() {
        when(wallets.getWalletById(ownWallet)).thenReturn(wallet(ownWallet, user));
        when(wallets.getWalletById(otherWallet)).thenReturn(wallet(otherWallet, otherUser));
        when(wallets.getWalletByUserId(user)).thenReturn(wallet(ownWallet, user));
        when(wallets.getWalletByUserId(otherUser)).thenReturn(wallet(otherWallet, otherUser));
        when(withdraw.withdraw(any())).thenReturn(wallet(ownWallet, user));
        when(transfer.transfer(any())).thenReturn(wallet(ownWallet, user));
        mvc = MockMvcBuilders.standaloneSetup(
                new WalletController(create, withdraw, wallets, transfer, transactions, lookupRecipient),
                new TransactionController(transactions, wallets),
                new PaymentController(payment, webhook))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    private WalletResponse wallet(UUID id, UUID owner) {
        return new WalletResponse(id, owner, new BigDecimal("1000000"), "VND", WalletStatus.ACTIVE, null, null);
    }


    private void status(int expected, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        assertEquals(expected, mvc.perform(request.principal(principal)).andReturn().getResponse().getStatus());
    }
    @Test public void cannotReadAnotherWallet() throws Exception {
        status(403, get("/wallet/" + otherWallet));
    }
    @Test public void cannotReadAnotherUsersWallet() throws Exception {
        status(403, get("/wallet/user/" + otherUser));
    }
    @Test public void ownerCanReadWallet() throws Exception {
        status(200, get("/wallet/" + ownWallet));
    }
    @Test public void cannotWithdrawFromAnotherWallet() throws Exception {
        status(403, post("/wallet/withdraw").contentType("application/json")
                .content("{\"walletId\":\"" + otherWallet + "\",\"amount\":1000}"));
        verifyNoInteractions(withdraw);
    }
    @Test public void ownerCanWithdraw() throws Exception {
        status(200, post("/wallet/withdraw").contentType("application/json")
                .content("{\"walletId\":\"" + ownWallet + "\",\"amount\":1000}"));
        verify(withdraw).withdraw(any());
    }
    @Test public void cannotTransferFromAnotherWallet() throws Exception {
        status(403, post("/wallet/transfer").contentType("application/json")
                .content("{\"senderWalletId\":\"" + otherWallet + "\",\"receiverWalletId\":\"" + ownWallet + "\",\"amount\":1000}"));
        verifyNoInteractions(transfer);
    }
    @Test public void ownerCanTransferToAnotherWallet() throws Exception {
        status(200, post("/wallet/transfer").contentType("application/json")
                .content("{\"senderWalletId\":\"" + ownWallet + "\",\"receiverWalletId\":\"" + otherWallet + "\",\"amount\":1000}"));
        verify(transfer).transfer(any());
    }
    @Test public void directDepositIsForbiddenEvenForOwner() throws Exception {
        status(403, post("/wallet/deposit").contentType("application/json")
                .content("{\"walletId\":\"" + ownWallet + "\",\"amount\":1000000}"));

    }
    @Test public void cannotReadAnotherWalletHistoryThroughEitherRoute() throws Exception {
        status(403, get("/wallet/" + otherWallet + "/transactions"));
        status(403, get("/transactions/wallet/" + otherWallet));
        verifyNoInteractions(transactions);
    }
    @Test public void cannotReadUnrelatedTransaction() throws Exception {
        UUID id = UUID.randomUUID();
        when(transactions.getTransactionById(id)).thenReturn(new TransactionResponse(id, TransactionType.DEPOSIT,
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, TransactionStatus.SUCCESS, "test", null, otherWallet, null));
        status(403, get("/transactions/" + id));
    }
    @Test public void paymentOrderUsesJwtUserInsteadOfBodyUser() throws Exception {
        when(payment.createPaymentOrder(any())).thenReturn(new PaymentOrderResponse(
                UUID.randomUUID(), ownWallet, user, null, new BigDecimal("1000"), PaymentGatewayType.STRIPE,
                "test-session", PaymentOrderStatus.PENDING, "https://example.com/checkout", null, null));
        assertEquals(200, mvc.perform(post("/payments/create-order").principal(principal).contentType("application/json")
                .content("{\"walletId\":\"" + ownWallet + "\",\"userId\":\"" + otherUser + "\",\"amount\":1000,\"gateway\":\"STRIPE\"}")).andReturn().getResponse().getStatus());
        ArgumentCaptor<CreatePaymentOrderCommand> command = ArgumentCaptor.forClass(CreatePaymentOrderCommand.class);
        verify(payment).createPaymentOrder(command.capture());
        assertEquals(user, command.getValue().userId());
    }
    @Test public void cannotCreateWalletForAnotherUser() throws Exception {
        status(403, post("/wallet").contentType("application/json")
                .content("{\"userId\":\"" + otherUser + "\",\"currency\":\"VND\"}"));
        verifyNoInteractions(create);
    }
    @Test public void ownerCanReadHistoryThroughBothRoutes() throws Exception {
        when(transactions.getTransactionsByWalletId(ownWallet)).thenReturn(List.of());
        status(200, get("/wallet/" + ownWallet + "/transactions"));
        status(200, get("/transactions/wallet/" + ownWallet));
    }
    @Test public void receiverCanReadTheirTransferTransaction() throws Exception {
        UUID id = UUID.randomUUID();
        when(transactions.getTransactionById(id)).thenReturn(new TransactionResponse(id, TransactionType.TRANSFER_OUT,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, TransactionStatus.SUCCESS, "test", otherWallet, ownWallet, null));
        status(200, get("/transactions/" + id));
    }
    @Test public void senderCanReadTheirTransferTransaction() throws Exception {
        UUID id = UUID.randomUUID();
        when(transactions.getTransactionById(id)).thenReturn(new TransactionResponse(id, TransactionType.TRANSFER_OUT,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, TransactionStatus.SUCCESS, "test", ownWallet, otherWallet, null));
        status(200, get("/transactions/" + id));
    }
    @Test public void missingPrincipalCannotWithdraw() throws Exception {
        assertEquals(403, mvc.perform(post("/wallet/withdraw").contentType("application/json")
                .content("{\"walletId\":\"" + ownWallet + "\",\"amount\":1000}"))
                .andReturn().getResponse().getStatus());
        verifyNoInteractions(withdraw);
    }
    @Test public void stripeWebhookStillReachesPaymentVerificationWithoutUserPrincipal() throws Exception {
        assertEquals(200, mvc.perform(post("/payments/webhook/stripe").contentType("application/json")
                .header("Stripe-Signature", "test-signature").content("{\"type\":\"test\"}"))
                .andReturn().getResponse().getStatus());
        var command = ArgumentCaptor.forClass(ProcessPaymentWebhookCommand.class);
        verify(webhook).processWebhook(command.capture());
        assertEquals(PaymentGatewayType.STRIPE, command.getValue().gateway());
        assertEquals("{\"type\":\"test\"}", command.getValue().rawPayload());
    }

    @Test public void payOsWebhookStillReachesPaymentVerificationWithoutUserPrincipal() throws Exception {
        assertEquals(200, mvc.perform(post("/payments/webhook/payos").contentType("application/json")
                .content("{\"code\":\"00\"}"))
                .andReturn().getResponse().getStatus());
        var command = ArgumentCaptor.forClass(ProcessPaymentWebhookCommand.class);
        verify(webhook, atLeastOnce()).processWebhook(command.capture());
        assertEquals(PaymentGatewayType.PAYOS, command.getValue().gateway());
        assertEquals("{\"code\":\"00\"}", command.getValue().rawPayload());
    }

    @Test public void lookupRecipientReturnsOkWhenFound() throws Exception {
        when(lookupRecipient.lookup(eq(user), eq("0901234567"))).thenReturn(
                new RecipientLookupResponse(
                        UUID.randomUUID(), UUID.randomUUID(), "NGUYEN VAN B", "0901234567", "b@example.com", "VND")
        );
        status(200, get("/wallet/lookup").param("identifier", "0901234567"));
    }
}
