package com.legalai.config;

import com.legalai.admin.service.AdminDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(NotificationHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private AdminDataService adminDataService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);
        if (token == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Long userId = validateToken(token);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        sessions.put(userId.toString(), session);
        session.getAttributes().put("userId", userId);
        log.info("[WS] Client connected: userId={}", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try {
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
            String action = node.has("action") ? node.get("action").asText() : null;

            if ("ping".equals(action)) {
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            }
        } catch (Exception e) {
            log.warn("[WS] Invalid message: {}", payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Object userId = session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(userId.toString());
            log.info("[WS] Client disconnected: userId={}", userId);
        }
    }

    public void sendToUser(Long userId, String message) {
        WebSocketSession session = sessions.get(userId.toString());
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                log.warn("[WS] Failed to send to user {}: {}", userId, e.getMessage());
            }
        }
    }

    public void broadcast(String message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    log.warn("[WS] Broadcast error: {}", e.getMessage());
                }
            }
        });
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if (kv.length == 2 && "token".equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    private Long validateToken(String token) {
        if (token == null || token.isEmpty()) return null;
        try {
            var rows = adminDataService.jdbc().queryForList(
                "SELECT user_id FROM auth_tokens WHERE token = ? AND expire_at > NOW()",
                token);
            if (rows.isEmpty()) return null;
            return Long.valueOf(rows.get(0).get("user_id").toString());
        } catch (Exception e) {
            log.warn("[WS] Token validation failed: {}", e.getMessage());
            return null;
        }
    }
}
