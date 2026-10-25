package com.oxtore.backend.notification.service;

import com.oxtore.backend.notification.enums.NotificationType;

import java.util.Map;
import java.util.Set;

/** Published inside the business transaction; handled only after it commits. */
public record NotificationRequestedEvent(
        Set<Long> recipientIds,
        NotificationType type,
        String title,
        String body,
        String icon,
        Map<String, String> meta
) {}
