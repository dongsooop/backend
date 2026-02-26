package com.dongsoop.dongsoop.chat.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private final Map<Long, String> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();

    public void addUserSession(Long userId, String sessionId) {
        userSessions.put(userId, sessionId);
        sessionToUser.put(sessionId, userId);
    }

    public void removeSession(String sessionId) {
        Long userId = sessionToUser.remove(sessionId);
        if (userId != null) {
            userSessions.computeIfPresent(userId, (key, currentSession) ->
                    sessionId.equals(currentSession) ? null : currentSession);
        }
    }

    public boolean isUserOnline(Long userId) {
        return userSessions.containsKey(userId);
    }
}