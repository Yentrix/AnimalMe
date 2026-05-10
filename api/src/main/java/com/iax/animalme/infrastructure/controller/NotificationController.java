package com.iax.animalme.infrastructure.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iax.animalme.application.service.NotificationApplicationService;
import com.iax.animalme.domain.model.Notification;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationApplicationService notificationApplicationService;

    public NotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> list(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(notificationApplicationService.listForUser(userId));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markAsRead(
            @PathVariable Long notificationId,
            @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(notificationApplicationService.markAsRead(notificationId, userId));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestParam("userId") Long userId) {
        notificationApplicationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
