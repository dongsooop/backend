package com.dongsoop.dongsoop.chat;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisChatRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private ZSetOperations<String, Object> zSetOperations;
    @Mock
    private SetOperations<String, Object> setOperations;

    private RedisChatRepository redisChatRepository;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        redisChatRepository = new RedisChatRepository(redisTemplate);
    }

    @Test
    @DisplayName("saveWithTTL - 원자적 set 호출로 TTL 포함 저장")
    void saveRoom_usesAtomicSetWithTTL() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .participants(new HashSet<>(Set.of(1L)))
                .build();

        redisChatRepository.saveRoom(room);

        verify(valueOperations).set(eq("chat:room:room1"), eq(room), eq(30L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("saveRoom - active SET에 roomId 추가")
    void saveRoom_addsToActiveSet() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .participants(new HashSet<>(Set.of(1L)))
                .build();

        redisChatRepository.saveRoom(room);

        verify(setOperations).add("chat:rooms:active", "room1");
    }

    @Test
    @DisplayName("deleteRoom - active SET에서 roomId 제거")
    void deleteRoom_removesFromActiveSet() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .participants(new HashSet<>(Set.of(1L)))
                .build();
        when(valueOperations.get("chat:room:room1")).thenReturn(room);
        when(zSetOperations.range(eq("chat:messages:sorted:room1"), eq(0L), eq(-1L)))
                .thenReturn(Collections.emptySet());

        redisChatRepository.deleteRoom("room1");

        verify(setOperations).remove("chat:rooms:active", "room1");
    }

    @Test
    @DisplayName("deleteRoomMessages - ZSET 기반으로 메시지 키 삭제")
    void deleteRoomMessages_usesZSetForKeys() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .participants(new HashSet<>(Set.of(1L)))
                .build();
        when(valueOperations.get("chat:room:room1")).thenReturn(room);

        Set<Object> messageIds = new LinkedHashSet<>(List.of("msg1", "msg2"));
        when(zSetOperations.range("chat:messages:sorted:room1", 0, -1)).thenReturn(messageIds);

        redisChatRepository.deleteRoom("room1");

        verify(redisTemplate).delete(List.of("chat:message:room1:msg1", "chat:message:room1:msg2"));
    }

    @Test
    @DisplayName("loadMessagesFromIds - multiGet으로 일괄 조회")
    void findMessagesByRoomId_usesMultiGet() {
        Set<Object> messageIds = new LinkedHashSet<>(List.of("msg1", "msg2"));
        when(zSetOperations.range("chat:messages:sorted:room1", 0, -1)).thenReturn(messageIds);

        ChatMessage msg1 = ChatMessage.builder().messageId("msg1").content("hello").build();
        ChatMessage msg2 = ChatMessage.builder().messageId("msg2").content("world").build();
        when(valueOperations.multiGet(List.of("chat:message:room1:msg1", "chat:message:room1:msg2")))
                .thenReturn(List.of(msg1, msg2));

        List<ChatMessage> result = redisChatRepository.findMessagesByRoomId("room1");

        assertThat(result).hasSize(2);
        verify(valueOperations).multiGet(List.of("chat:message:room1:msg1", "chat:message:room1:msg2"));
    }

    @Test
    @DisplayName("loadMessagesFromIds - null 결과 필터링")
    void findMessagesByRoomId_filtersNulls() {
        Set<Object> messageIds = new LinkedHashSet<>(List.of("msg1", "msg2"));
        when(zSetOperations.range("chat:messages:sorted:room1", 0, -1)).thenReturn(messageIds);

        ChatMessage msg1 = ChatMessage.builder().messageId("msg1").content("hello").build();
        List<Object> results = new ArrayList<>();
        results.add(msg1);
        results.add(null);
        when(valueOperations.multiGet(anyList())).thenReturn(results);

        List<ChatMessage> result = redisChatRepository.findMessagesByRoomId("room1");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findRoomsWithLastActivityBefore - active SET 순회")
    void findRoomsWithLastActivityBefore_usesActiveSet() {
        LocalDateTime cutoff = LocalDateTime.now();
        Set<Object> activeIds = new HashSet<>(Set.of("room1", "room2"));
        when(setOperations.members("chat:rooms:active")).thenReturn(activeIds);

        ChatRoom room1 = ChatRoom.builder()
                .roomId("room1")
                .lastActivityAt(cutoff.minusDays(1))
                .build();
        ChatRoom room2 = ChatRoom.builder()
                .roomId("room2")
                .lastActivityAt(cutoff.plusDays(1))
                .build();
        when(valueOperations.get("chat:room:room1")).thenReturn(room1);
        when(valueOperations.get("chat:room:room2")).thenReturn(room2);

        List<ChatRoom> result = redisChatRepository.findRoomsWithLastActivityBefore(cutoff);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoomId()).isEqualTo("room1");
        verify(redisTemplate, never()).keys(any());
    }

    @Test
    @DisplayName("saveMessage - 개별 키 저장 시 TTL 포함")
    void saveMessage_savesWithTTL() {
        ChatMessage message = ChatMessage.builder()
                .messageId("msg1")
                .roomId("room1")
                .senderId(1L)
                .content("hello")
                .timestamp(LocalDateTime.now())
                .type(MessageType.CHAT)
                .build();

        redisChatRepository.saveMessage(message);

        verify(valueOperations).set(eq("chat:message:room1:msg1"), eq(message), eq(30L), eq(TimeUnit.DAYS));
        verify(zSetOperations).add(eq("chat:messages:sorted:room1"), eq("msg1"), anyDouble());
    }
}
