package com.dongsoop.dongsoop.chat;

import com.dongsoop.dongsoop.chat.service.ReadStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadStatusServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ReadStatusService readStatusService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        readStatusService = new ReadStatusService(redisTemplate);
    }

    @Test
    @DisplayName("getLastReadTimestampsBatch - pipeline으로 일괄 조회")
    void getLastReadTimestampsBatch_usesMultiGet() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        when(valueOperations.multiGet(List.of("user:lastread:1:room1", "user:lastread:1:room2")))
                .thenReturn(List.of(timestamp, timestamp));

        Map<String, LocalDateTime> result = readStatusService.getLastReadTimestampsBatch(1L, List.of("room1", "room2"));

        assertThat(result).hasSize(2);
        assertThat(result.get("room1")).isNotNull();
        assertThat(result.get("room2")).isNotNull();
        verify(valueOperations).multiGet(List.of("user:lastread:1:room1", "user:lastread:1:room2"));
    }

    @Test
    @DisplayName("getLastReadTimestampsBatch - null이면 joinTime으로 fallback")
    void getLastReadTimestampsBatch_fallsBackToJoinTime() {
        LocalDateTime joinTime = LocalDateTime.now().minusHours(1);
        String joinTimeStr = joinTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        List<String> nullList = new java.util.ArrayList<>();
        nullList.add(null);
        when(valueOperations.multiGet(List.of("user:lastread:1:room1"))).thenReturn(nullList);

        List<String> joinTimeList = new java.util.ArrayList<>();
        joinTimeList.add(joinTimeStr);
        when(valueOperations.multiGet(List.of("user:jointime:1:room1"))).thenReturn(joinTimeList);

        Map<String, LocalDateTime> result = readStatusService.getLastReadTimestampsBatch(1L, List.of("room1"));

        assertThat(result.get("room1")).isEqualTo(joinTime);
    }

    @Test
    @DisplayName("getLastReadTimestampsBatchForUsers - 사용자별 일괄 조회")
    void getLastReadTimestampsBatchForUsers_usesMultiGet() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        when(valueOperations.multiGet(List.of("user:lastread:1:room1", "user:lastread:2:room1")))
                .thenReturn(List.of(timestamp, timestamp));

        Map<Long, LocalDateTime> result = readStatusService.getLastReadTimestampsBatchForUsers(
                List.of(1L, 2L), "room1");

        assertThat(result).hasSize(2);
        verify(valueOperations).multiGet(List.of("user:lastread:1:room1", "user:lastread:2:room1"));
    }

    @Test
    @DisplayName("updateLastReadTimestamp - Redis에 TTL과 함께 저장")
    void updateLastReadTimestamp_savesWithTTL() {
        LocalDateTime now = LocalDateTime.now();

        readStatusService.updateLastReadTimestamp(1L, "room1", now);

        verify(valueOperations).set(eq("user:lastread:1:room1"), anyString(), eq(30L), any());
    }

    @Test
    @DisplayName("initializeUserReadStatus - 읽음 시간과 참가 시간 모두 저장")
    void initializeUserReadStatus_savesBothTimestamps() {
        LocalDateTime joinTime = LocalDateTime.now();

        readStatusService.initializeUserReadStatus(1L, "room1", joinTime);

        verify(valueOperations, times(2)).set(anyString(), anyString(), eq(30L), any());
    }
}
