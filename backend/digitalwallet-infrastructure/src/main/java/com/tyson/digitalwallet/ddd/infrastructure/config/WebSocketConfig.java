package com.tyson.digitalwallet.ddd.infrastructure.config;

import com.tyson.digitalwallet.ddd.application.port.security.TokenProvider;
import com.tyson.digitalwallet.ddd.application.port.security.TokenStorage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collections;
import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final TokenProvider tokenProvider;
    private final TokenStorage tokenStorage;

    public WebSocketConfig(TokenProvider tokenProvider, TokenStorage tokenStorage) {
        this.tokenProvider = tokenProvider;
        this.tokenStorage = tokenStorage;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");

        registry.setApplicationDestinationPrefixes("/app");

        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) {
                    throw new AccessDeniedException("STOMP headers are required");
                }
                StompCommand command = accessor.getCommand();
                if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
                    authenticate(accessor);
                } else if (StompCommand.SEND.equals(command)) {
                    // This socket is receive-only; clients must never publish broker messages.
                    throw new AccessDeniedException("Client messages are not allowed");
                } else if (StompCommand.SUBSCRIBE.equals(command)) {
                    if (accessor.getUser() == null
                            || !"/user/queue/notifications".equals(accessor.getDestination())) {
                        throw new AccessDeniedException("Only personal notifications are allowed");
                    }
                }
                return message;
            }
        });
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ") || header.substring(7).isBlank()) {
            throw new AccessDeniedException("A bearer token is required");
        }
        try {
            String token = header.substring(7);
            if (!tokenProvider.validateToken(token) || tokenStorage.isBlacklistToken(token)) {
                throw new AccessDeniedException("Invalid WebSocket credentials");
            }
            UUID userId = tokenProvider.extractUserId(token);
            String sessionId = tokenProvider.extractSessionId(token);
            if (userId == null || sessionId == null
                    || !tokenStorage.isValidSession(userId, sessionId)) {
                throw new AccessDeniedException("Invalid WebSocket session");
            }
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userId.toString(), null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))));
        } catch (RuntimeException ex) {
            // Fail closed, including when token parsing or session validation fails.
            throw new AccessDeniedException("WebSocket authentication failed", ex);
        }
    }
}
