package com.oxtore.backend.notification.realtime;

import com.oxtore.backend.notification.DTO.NotificationResponse;

/**
 * Delivery layer for already-persisted notifications. Socket.IO today; an SSE implementation can replace it
 * without touching NotificationService or the business hooks.
 */
public interface NotificationDispatcher {

    /** Push to every live connection of {@code notification.userId()}. Must not throw. */
    void dispatch(NotificationResponse notification);
}
