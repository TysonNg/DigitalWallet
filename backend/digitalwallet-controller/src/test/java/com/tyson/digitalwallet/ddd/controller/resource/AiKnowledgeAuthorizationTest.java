package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.ai.ChatFinancialAssistantUseCase;
import org.junit.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AiKnowledgeAuthorizationTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        AiAssistantController controller() {
            return new AiAssistantController(mock(ChatFinancialAssistantUseCase.class), () -> 3, Runnable::run);
        }
    }

    @Test
    public void regularUserCannotSyncButAdminCan() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(AiAssistantController.class);
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "", "ROLE_USER"));
            assertThrows(AccessDeniedException.class, controller::syncKnowledge);
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "", "ROLE_ADMIN"));
            assertEquals(200, controller.syncKnowledge().getStatusCode().value());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
