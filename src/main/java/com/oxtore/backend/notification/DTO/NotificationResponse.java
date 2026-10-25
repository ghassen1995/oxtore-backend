package com.oxtore.backend.notification.DTO;

import com.oxtore.backend.notification.entity.Notification;

import java.util.Map;

/**
 * Wire shape shared by REST and the realtime channel. Ids and dates are pre-rendered as strings because
 * netty-socketio serializes with its own Jackson 2 mapper, which doesn't see the app's Jackson 3 config.
 */
public record NotificationResponse(
        String id,
        String userId,
        String type,
        String title,
        String body,
        String icon,
        boolean read,
        Map<String, String> meta,
        String createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                String.valueOf(n.getId()),
                String.valueOf(n.getUserId()),
                n.getType().apiValue(),
                n.getTitle(),
                n.getBody(),
                n.getIcon(),
                n.isRead(),
                n.getMeta() != null ? Map.copyOf(n.getMeta()) : Map.of(),
                n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
    }
}
