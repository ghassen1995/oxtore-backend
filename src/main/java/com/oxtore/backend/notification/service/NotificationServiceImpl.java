package com.oxtore.backend.notification.service;

import com.oxtore.backend.notification.DTO.NotificationResponse;
import com.oxtore.backend.notification.entity.Notification;
import com.oxtore.backend.notification.enums.NotificationType;
import com.oxtore.backend.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void notify(Collection<Long> recipientIds, Long actorId, NotificationType type,
                       String title, String body, String icon, Map<String, String> meta) {
        try {
            Set<Long> recipients = new LinkedHashSet<>();
            if (recipientIds != null) {
                recipientIds.stream().filter(Objects::nonNull).forEach(recipients::add);
            }
            if (actorId != null) {
                recipients.remove(actorId);
            }
            if (recipients.isEmpty()) {
                return;
            }
            eventPublisher.publishEvent(new NotificationRequestedEvent(
                    recipients, type, title, body, icon, meta != null ? Map.copyOf(meta) : Map.of()));
        } catch (RuntimeException e) {
            log.error("Failed to queue {} notification '{}'", type, title, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Long userId, int page, int limit) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(page - 1, limit))
                .map(NotificationResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification n = findOwned(userId, notificationId);
        n.setRead(true);
        return NotificationResponse.from(notificationRepository.save(n));
    }

    @Override
    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long notificationId) {
        notificationRepository.delete(findOwned(userId, notificationId));
    }

    /** Someone else's notification is reported as not found, so ids can't be probed. */
    private Notification findOwned(Long userId, Long notificationId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + notificationId));
    }
}
