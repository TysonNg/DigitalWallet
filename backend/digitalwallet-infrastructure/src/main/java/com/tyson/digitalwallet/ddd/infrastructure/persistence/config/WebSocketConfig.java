package com.tyson.digitalwallet.ddd.infrastructure.persistence.config;

import com.tyson.digitalwallet.ddd.domain.service.security.TokenProvider;
import com.tyson.digitalwallet.ddd.domain.service.security.TokenStorage;
import org.jspecify.annotations.Nullable;
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

    private TokenProvider tokenProvider;
    private TokenStorage tokenStorage;

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
    public void configureMessageBroker(MessageBrokerRegistry registry){
        registry.enableSimpleBroker("/topic", "/queue");

        registry.setApplicationDestinationPrefixes("/app");

        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public  Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if(accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Lấy thông tin Header của frame STOMP
                    String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

                    // Chỉ can thiệp khi Client gửi lệnh CONNECT lần đầu tiên
                    if(authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);

                        // 1. Kiểm tra JWT hợp lệ
                        if (tokenProvider.validateToken(token) && !tokenStorage.isBlacklistToken(token)) {
                            UUID userId = tokenProvider.extractUserId(token);
                            String sessionId = tokenProvider.extractSessionId(token);

                            // 2. Kiểm tra Session Redis
                            if (sessionId != null && tokenStorage.isValidSession(userId, sessionId)) {
                                // 3. Tạo thông tin xác thực
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                userId.toString(), // Name của Principal chính là userId
                                                null,
                                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                        );

                                // 4. GÁN USER VÀO WEBSOCKET SESSION
                                // Nhờ dòng này, Spring biết kết nối này là của User ID nào!
                                accessor.setUser(authentication);
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}
