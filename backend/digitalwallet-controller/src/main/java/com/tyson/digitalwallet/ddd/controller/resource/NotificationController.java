package com.tyson.digitalwallet.ddd.controller.resource;

import com.tyson.digitalwallet.ddd.application.usecase.notification.NotificationUseCase;
import com.tyson.digitalwallet.ddd.controller.common.ApiResponse;
import com.tyson.digitalwallet.ddd.controller.dto.response.notification.NotificationResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    public NotificationController(NotificationUseCase notificationUseCase) {
        this.notificationUseCase = notificationUseCase;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getNotificationsByUserId(
            @PathVariable UUID userId
    ) {
        List<NotificationResponseDto> notifications = notificationUseCase.getNotificationsByUserId(userId).stream()
                .map(NotificationResponseDto::from)
                .toList();
        return ApiResponse.ok("Get notifications successfully", notifications);
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @PathVariable UUID userId
    ) {
        long count = notificationUseCase.countUnreadByUserId(userId);
        return ApiResponse.ok("Get unread notifications count successfully", count);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID id,
            @RequestParam UUID userId
    ) {
        notificationUseCase.markAsRead(id, userId);
        return ApiResponse.ok("Notification marked as read successfully", null);
    }

    @PatchMapping("/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @PathVariable UUID userId
    ) {
        notificationUseCase.markAllAsRead(userId);
        return ApiResponse.ok("All notifications marked as read successfully", null);
    }
}
