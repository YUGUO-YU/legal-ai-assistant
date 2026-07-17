package com.legalai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.legalai.config.NotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_RECENT = 100;

    private final Map<Long, ConcurrentLinkedQueue<String>> pendingNotifications = new ConcurrentHashMap<>();

    @Autowired
    private NotificationHandler notificationHandler;

    public void sendToUser(Long userId, String type, String title, String message) {
        String json = buildNotification(type, title, message);
        notificationHandler.sendToUser(userId, json);

        ConcurrentLinkedQueue<String> queue = pendingNotifications.computeIfAbsent(userId, k -> new ConcurrentLinkedQueue<>());
        queue.offer(json);
        while (queue.size() > MAX_RECENT) {
            queue.poll();
        }
    }

    public void broadcast(String type, String title, String message) {
        String json = buildNotification(type, title, message);
        notificationHandler.broadcast(json);
    }

    public ConcurrentLinkedQueue<String> getPendingNotifications(Long userId) {
        return pendingNotifications.get(userId);
    }

    public void clearPendingNotifications(Long userId) {
        pendingNotifications.remove(userId);
    }

    private String buildNotification(String type, String title, String message) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("type", type);
        node.put("title", title);
        node.put("message", message);
        node.put("read", false);
        node.put("createdAt", System.currentTimeMillis());
        return node.toString();
    }
}
