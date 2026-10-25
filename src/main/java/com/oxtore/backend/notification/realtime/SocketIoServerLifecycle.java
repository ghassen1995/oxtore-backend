package com.oxtore.backend.notification.realtime;

import com.corundumstudio.socketio.AuthTokenResult;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.oxtore.backend.identity.config.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Standalone Socket.IO (v4) server on its own port. Not behind the Spring Security filter chain,
 * so the JWT from the handshake auth payload is validated here with the same JwtUtil as JwtAuthFilter.
 * Each authenticated connection joins the room "user:{id}", which is what the dispatcher targets.
 */
@Slf4j
@Component
public class SocketIoServerLifecycle implements SmartLifecycle {

    public static final String NAMESPACE = "/notifications";
    public static final String EVENT_NEW = "notification:new";
    private static final String USER_ID_KEY = "userId";

    private final JwtUtil jwtUtil;
    private final String host;
    private final int port;

    private volatile SocketIOServer server;
    private volatile SocketIONamespace namespace;

    public SocketIoServerLifecycle(JwtUtil jwtUtil,
                                   @Value("${socketio.host:0.0.0.0}") String host,
                                   @Value("${socketio.port:8082}") int port) {
        this.jwtUtil = jwtUtil;
        this.host = host;
        this.port = port;
    }

    public static String userRoom(Long userId) {
        return "user:" + userId;
    }

    /** Null until the server has started. */
    public SocketIONamespace namespace() {
        return namespace;
    }

    @Override
    public void start() {
        Configuration config = new Configuration();
        config.setHostname(host);
        config.setPort(port);
        // Same policy as the HTTP CORS config (allowed origin patterns "*").
        config.setOrigin("*");

        SocketIOServer s = new SocketIOServer(config);
        SocketIONamespace ns = s.addNamespace(NAMESPACE);

        // Invoked with the CONNECT packet's auth payload ({ token }) for Socket.IO v4 clients.
        ns.addAuthTokenListener((authData, client) -> {
            Long userId = userIdFromAuth(authData);
            if (userId == null) {
                return new AuthTokenResult(false, Map.of("message", "unauthorized"));
            }
            client.set(USER_ID_KEY, userId);
            return AuthTokenResult.AuthTokenResultSuccess;
        });

        // The auth listener is skipped when a client sends no auth payload at all, so re-check here.
        ns.addConnectListener(client -> {
            Long userId = client.get(USER_ID_KEY);
            if (userId == null) {
                log.debug("Socket.IO connection {} without a valid token, disconnecting", client.getSessionId());
                client.disconnect();
                return;
            }
            client.joinRoom(userRoom(userId));
        });

        s.start();
        this.server = s;
        this.namespace = ns;
        log.info("Socket.IO notifications server started on {}:{} (namespace {})", host, port, NAMESPACE);
    }

    private Long userIdFromAuth(Object authData) {
        if (!(authData instanceof Map<?, ?> map) || !(map.get("token") instanceof String token) || token.isBlank()) {
            return null;
        }
        try {
            return jwtUtil.isValid(token) ? jwtUtil.extractUserId(token) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public void stop() {
        SocketIOServer s = this.server;
        this.namespace = null;
        this.server = null;
        if (s != null) {
            s.stop();
            log.info("Socket.IO notifications server stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return server != null;
    }
}
