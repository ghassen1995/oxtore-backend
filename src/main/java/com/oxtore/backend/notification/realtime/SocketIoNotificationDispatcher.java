package com.oxtore.backend.notification.realtime;

import com.corundumstudio.socketio.SocketIONamespace;
import com.oxtore.backend.notification.DTO.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocketIoNotificationDispatcher implements NotificationDispatcher {

    private final SocketIoServerLifecycle socketServer;

    @Override
    public void dispatch(NotificationResponse notification) {
        try {
            SocketIONamespace ns = socketServer.namespace();
            if (ns == null) {
                log.warn("Socket.IO server not running; notification {} stays unpushed (still persisted)", notification.id());
                return;
            }
            ns.getRoomOperations(SocketIoServerLifecycle.userRoom(Long.valueOf(notification.userId())))
                    .sendEvent(SocketIoServerLifecycle.EVENT_NEW, notification);
        } catch (RuntimeException e) {
            log.warn("Failed to push notification {} to user {}", notification.id(), notification.userId(), e);
        }
    }
}
