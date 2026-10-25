package com.oxtore.backend.notification.service;

import com.oxtore.backend.notification.DTO.NotificationResponse;
import com.oxtore.backend.notification.enums.NotificationType;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.Map;

public interface NotificationService {

    /**
     * Notify {@code recipientIds} (minus {@code actorId}, de-duplicated). Persisted and pushed only after the
     * caller's transaction commits (immediately if there is none). Never throws: failures are logged.
     */
    void notify(Collection<Long> recipientIds, Long actorId, NotificationType type,
                String title, String body, String icon, Map<String, String> meta);

    Page<NotificationResponse> list(Long userId, int page, int limit);

    long unreadCount(Long userId);

    NotificationResponse markRead(Long userId, Long notificationId);

    int markAllRead(Long userId);

    void delete(Long userId, Long notificationId);
}
