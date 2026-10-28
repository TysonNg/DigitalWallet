package com.tyson.digitalwallet.ddd.infrastructure.config;

import com.tyson.digitalwallet.ddd.application.port.security.TokenProvider;
import com.tyson.digitalwallet.ddd.application.port.security.TokenStorage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import java.util.UUID;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WebSocketSecurityTest {
    private TokenProvider tokens;
    private TokenStorage storage;
    private ChannelInterceptor interceptor;
    private final UUID user = UUID.randomUUID();

    @Before
    public void setup() {
        tokens = mock(TokenProvider.class);
        storage = mock(TokenStorage.class);
        var registration = mock(ChannelRegistration.class);
        new WebSocketConfig(tokens, storage).configureClientInboundChannel(registration);
        var capture = ArgumentCaptor.forClass(ChannelInterceptor.class);
        verify(registration).interceptors(capture.capture());
        interceptor = capture.getValue();
    }

    private void send(StompHeaderAccessor headers) {
        interceptor.preSend(MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()), null);
    }

    private StompHeaderAccessor validConnection() {
        when(tokens.validateToken("valid")).thenReturn(true);
        when(tokens.extractUserId("valid")).thenReturn(user);
        when(tokens.extractSessionId("valid")).thenReturn("session");
        when(storage.isValidSession(user, "session")).thenReturn(true);
        var headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.setNativeHeader("Authorization", "Bearer valid");
        headers.setLeaveMutable(true);
        return headers;
    }

    @Test
    public void missingAndInvalidTokensAreRejected() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> send(StompHeaderAccessor.create(StompCommand.CONNECT)));
        var invalid = StompHeaderAccessor.create(StompCommand.CONNECT);
        invalid.setNativeHeader("Authorization", "Bearer invalid");
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> send(invalid));
    }

    @Test
    public void revokedTokenAndInactiveSessionAreRejected() {
        var revoked = validConnection();
        when(storage.isBlacklistToken("valid")).thenReturn(true);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> send(revoked));
        var inactive = validConnection();
        when(storage.isBlacklistToken("valid")).thenReturn(false);
        when(storage.isValidSession(user, "session")).thenReturn(false);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> send(inactive));
    }

    @Test
    public void validConnectionBindsAuthenticatedUser() {
        var headers = validConnection();
        send(headers);
        assertEquals(user.toString(), headers.getUser().getName());
    }

    @Test
    public void onlyAuthenticatedPersonalNotificationSubscriptionIsAllowed() {
        var own = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        own.setLeaveMutable(true);
        own.setDestination("/user/queue/notifications");
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> send(own));
        own.setUser(() -> user.toString());
        send(own);
        for (String destination : java.util.List.of("/queue/notifications", "/topic/test", "/user/other/queue/notifications", "/queue/notifications-user-session")) {
            var other = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            other.setUser(() -> user.toString());
            other.setDestination(destination);
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> send(other));
        }
    }

    @Test
    public void clientCannotForgeNotifications() {
        for (String destination : java.util.List.of("/queue/notifications", "/topic/test", "/user/queue/notifications", "/app/test")) {
            var headers = StompHeaderAccessor.create(StompCommand.SEND);
            headers.setUser(() -> user.toString());
            headers.setDestination(destination);
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> send(headers));
        }
    }
}
