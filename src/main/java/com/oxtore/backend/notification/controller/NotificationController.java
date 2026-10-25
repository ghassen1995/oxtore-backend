package com.oxtore.backend.notification.controller;

import com.oxtore.backend.notification.DTO.NotificationResponse;
import com.oxtore.backend.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Notifications of the authenticated user only — no cross-user access. Shape matches notifications.api.ts. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "page", required = false) Integer page,
            HttpServletRequest http) {
        Long userId = currentUserId(http);
        int size = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        int pageNumber = page == null ? 1 : Math.max(page, 1);

        Page<NotificationResponse> result = notificationService.list(userId, pageNumber, size);
        // Counts go out as int: the global Jackson config renders every Long/long as a string.
        return ResponseEntity.ok(Map.of(
                "data", result.getContent(),
                "meta", Map.of(
                        "unread", Math.toIntExact(notificationService.unreadCount(userId)),
                        "total", Math.toIntExact(result.getTotalElements()),
                        "page", pageNumber,
                        "totalPages", result.getTotalPages())));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Object>> unreadCount(HttpServletRequest http) {
        return ResponseEntity.ok(Map.of("count", Math.toIntExact(notificationService.unreadCount(currentUserId(http)))));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable("id") Long id, HttpServletRequest http) {
        return ResponseEntity.ok(notificationService.markRead(currentUserId(http), id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(HttpServletRequest http) {
        return ResponseEntity.ok(Map.of("updated", notificationService.markAllRead(currentUserId(http))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, HttpServletRequest http) {
        notificationService.delete(currentUserId(http), id);
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId(HttpServletRequest http) {
        Object userId = http.getAttribute("userId");
        if (userId == null) {
            throw new AccessDeniedException("No authenticated user found on request");
        }
        return Long.valueOf(userId.toString());
    }
}
