package com.dongsoop.dongsoop.chat.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionManager {

    private final Map<Long, String> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();

    public void addUserSession(Long userId, String sessionId) {
        String previousSessionId = userSessions.put(userId, sessionId);
        if (previousSessionId != null && !previousSessionId.equals(sessionId)) {
            sessionToUser.remove(previousSessionId);
        }
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
        return userId != null && userSessions.containsKey(userId);
    }
}