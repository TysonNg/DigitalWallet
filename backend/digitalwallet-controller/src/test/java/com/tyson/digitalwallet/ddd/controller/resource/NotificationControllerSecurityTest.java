package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.notification.NotificationUseCase;
import com.tyson.digitalwallet.ddd.controller.exception.GlobalExceptionHandler;
import org.junit.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.UUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

public class NotificationControllerSecurityTest {
    @Test
    public void rejectsAccessToAnotherUsersNotificationsOnEveryRoute() throws Exception {
        var useCase = mock(NotificationUseCase.class);
        var mvc = MockMvcBuilders.standaloneSetup(new NotificationController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var owner = UUID.randomUUID();
        var attacker = UUID.randomUUID();
        var requests = java.util.List.of(
                get("/notifications/user/" + owner),
                get("/notifications/user/" + owner + "/unread-count"),
                patch("/notifications/user/" + owner + "/read-all"),
                patch("/notifications/" + UUID.randomUUID() + "/read").param("userId", owner.toString()));
        for (var request : requests) {
            assertEquals(403, mvc.perform(request.principal(() -> attacker.toString()))
                    .andReturn().getResponse().getStatus());
        }
        verifyNoInteractions(useCase);
    }

    @Test
    public void permitsOwnerOnEveryRoute() throws Exception {
        var useCase = mock(NotificationUseCase.class);
        var mvc = MockMvcBuilders.standaloneSetup(new NotificationController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var owner = UUID.randomUUID();
        var notification = UUID.randomUUID();
        var requests = java.util.List.of(
                get("/notifications/user/" + owner),
                get("/notifications/user/" + owner + "/unread-count"),
                patch("/notifications/user/" + owner + "/read-all"),
                patch("/notifications/" + notification + "/read").param("userId", owner.toString()));
        for (var request : requests) {
            assertEquals(200, mvc.perform(request.principal(() -> owner.toString()))
                    .andReturn().getResponse().getStatus());
        }
        verify(useCase).markAsRead(notification, owner);
    }
}
