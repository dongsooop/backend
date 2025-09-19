package com.dongsoop.dongsoop.chat.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private final Map<Long, String> userSessions = new ConcurrentHashMap<>();

    public void addUserSession(Long userId, String sessionId) {
        userSessions.put(userId, sessionId);
    }

    public void removeSession(String sessionId) {
        userSessions.entrySet().removeIf(entry -> entry.getValue().equals(sessionId));
    }
}