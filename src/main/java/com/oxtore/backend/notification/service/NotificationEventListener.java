package com.oxtore.backend.notification.service;

import com.oxtore.backend.notification.DTO.NotificationResponse;
import com.oxtore.backend.notification.entity.Notification;
import com.oxtore.backend.notification.realtime.NotificationDispatcher;
import com.oxtore.backend.notification.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.List;

/**
 * Runs only after the business transaction commits (or right away when there was none), so a rolled-back
 * operation never produces a notification. Persists in its own transaction, then pushes; any failure is
 * logged and never reaches the business caller.
 */
@Slf4j
@Component
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher dispatcher;
    private final TransactionTemplate requiresNew;

    public NotificationEventListener(NotificationRepository notificationRepository,
                                     NotificationDispatcher dispatcher,
                                     PlatformTransactionManager transactionManager) {
        this.notificationRepository = notificationRepository;
        this.dispatcher = dispatcher;
        this.requiresNew = new TransactionTemplate(transactionManager);
        // The outer transaction has committed but is still bound to the thread; writes need a fresh one.
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationRequested(NotificationRequestedEvent event) {
        List<NotificationResponse> saved;
        try {
            saved = requiresNew.execute(status -> event.recipientIds().stream()
                    .map(userId -> notificationRepository.save(Notification.builder()
                            .userId(userId)
                            .type(event.type())
                            .title(event.title())
                            .body(event.body())
                            .icon(event.icon())
                            .read(false)
                            .meta(new HashMap<>(event.meta()))
                            .build()))
                    .map(NotificationResponse::from)
                    .toList());
        } catch (RuntimeException e) {
            log.error("Failed to persist {} notification '{}' for users {}",
                    event.type(), event.title(), event.recipientIds(), e);
            return;
        }
        // Pushed only once the rows are committed, so a client that refetches will see them.
        if (saved != null) {
            saved.forEach(dispatcher::dispatch);
        }
    }
}
