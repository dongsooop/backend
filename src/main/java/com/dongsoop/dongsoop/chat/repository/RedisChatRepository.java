package com.dongsoop.dongsoop.chat.repository;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
public class RedisChatRepository implements ChatRepository {
    private final static long CHAT_TTL = 30; // 30일 TTL
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisChatRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ChatRoom saveRoom(ChatRoom room) {
        String key = "chat:room:" + room.getRoomId();
        redisTemplate.opsForValue().set(key, room);
        redisTemplate.expire(key, CHAT_TTL, TimeUnit.DAYS);
        return room;
    }

    @Override
    public Optional<ChatRoom> findRoomById(String roomId) {
        String key = "chat:room:" + roomId;
        ChatRoom room = (ChatRoom) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(room);
    }

    @Override
    public Optional<ChatRoom> findRoomByParticipants(String user1, String user2) {
        Set<String> keys = redisTemplate.keys("chat:room:*");
        if (keys == null || keys.isEmpty()) {
            return Optional.empty();
        }

        for (String key : keys) {
            ChatRoom room = (ChatRoom) redisTemplate.opsForValue().get(key);
            if (room != null &&
                    room.getParticipants().size() == 2 &&
                    room.getParticipants().contains(user1) &&
                    room.getParticipants().contains(user2)) {
                return Optional.of(room);
            }
        }
        return Optional.empty();
    }

    @Override
    public void saveMessage(ChatMessage message) {
        String key = "chat:message:" + message.getRoomId() + ":" + message.getMessageId();
        redisTemplate.opsForValue().set(key, message);
        redisTemplate.expire(key, CHAT_TTL, TimeUnit.DAYS);

        // 메시지 목록에도 추가
        String listKey = "chat:messages:" + message.getRoomId();
        redisTemplate.opsForList().rightPush(listKey, message);
        redisTemplate.expire(listKey, CHAT_TTL, TimeUnit.DAYS);
    }

    @Override
    public List<ChatMessage> findMessagesByRoomId(String roomId) {
        String listKey = "chat:messages:" + roomId;
        List<Object> objects = redisTemplate.opsForList().range(listKey, 0, -1);
        if (objects == null) return Collections.emptyList();

        return objects.stream()
                .map(obj -> (ChatMessage) obj)
                .collect(Collectors.toList());
    }

    public List<ChatRoom> findRoomsOlderThan(int days) {
        Set<String> keys = redisTemplate.keys("chat:room:*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        // 실제 구현에서는 메타데이터를 통해 생성일을 저장하고 조회해야 함
        List<ChatRoom> rooms = new ArrayList<>();
        for (String key : keys) {
            ChatRoom room = (ChatRoom) redisTemplate.opsForValue().get(key);
            if (room != null) {
                rooms.add(room);
            }
        }
        return rooms;
    }

    public List<ChatRoom> findRoomsByUserId(String userId) {
        Set<String> keys = redisTemplate.keys("chat:room:*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatRoom> userRooms = new ArrayList<>();
        for (String key : keys) {
            ChatRoom room = (ChatRoom) redisTemplate.opsForValue().get(key);
            if (room != null && room.getParticipants().contains(userId)) {
                userRooms.add(room);
            }
        }
        return userRooms;
    }
}