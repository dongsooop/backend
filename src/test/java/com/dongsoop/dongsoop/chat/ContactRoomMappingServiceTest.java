package com.dongsoop.dongsoop.chat;

import com.dongsoop.dongsoop.chat.service.ContactRoomMappingService;
import com.dongsoop.dongsoop.search.entity.BoardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactRoomMappingServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ContactRoomMappingService contactRoomMappingService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        contactRoomMappingService = new ContactRoomMappingService(redisTemplate);
    }

    @Test
    @DisplayName("findExistingContactRoomId - 존재하는 매핑 반환")
    void findExistingContactRoomId_returnsRoomId() {
        when(valueOperations.get(anyString())).thenReturn("room-123");

        String result = contactRoomMappingService.findExistingContactRoomId(1L, 2L, BoardType.PROJECT, 10L);

        assertThat(result).isEqualTo("room-123");
    }

    @Test
    @DisplayName("findExistingContactRoomId - 매핑 없으면 null")
    void findExistingContactRoomId_returnsNull() {
        when(valueOperations.get(anyString())).thenReturn(null);

        String result = contactRoomMappingService.findExistingContactRoomId(1L, 2L, BoardType.PROJECT, 10L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("saveContactRoomMapping - 양방향 매핑 저장")
    void saveContactRoomMapping_savesBidirectional() {
        contactRoomMappingService.saveContactRoomMapping(1L, 2L, BoardType.PROJECT, 10L, "room-123");

        verify(valueOperations, times(2)).set(anyString(), anyString());
    }

    @Test
    @DisplayName("deleteContactRoomMapping - 역매핑에서 원래 매핑 키 조회 후 삭제")
    void deleteContactRoomMapping_deletesBoth() {
        when(valueOperations.get("room_to_contact:room-123")).thenReturn("1:2:PROJECT:10");

        contactRoomMappingService.deleteContactRoomMapping("room-123");

        verify(redisTemplate).delete("contact:1:2:PROJECT:10");
        verify(redisTemplate).delete("room_to_contact:room-123");
    }

    @Test
    @DisplayName("deleteContactRoomMapping - 역매핑 없으면 삭제 안함")
    void deleteContactRoomMapping_noReverseMapping() {
        when(valueOperations.get("room_to_contact:room-123")).thenReturn(null);

        contactRoomMappingService.deleteContactRoomMapping("room-123");

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("findExistingContactRoomId - userId 순서에 관계없이 동일한 키 사용")
    void findExistingContactRoomId_keyOrderIndependent() {
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(valueOperations.get(keyCaptor.capture())).thenReturn("room-123");

        contactRoomMappingService.findExistingContactRoomId(1L, 2L, BoardType.PROJECT, 10L);
        contactRoomMappingService.findExistingContactRoomId(2L, 1L, BoardType.PROJECT, 10L);

        List<String> capturedKeys = keyCaptor.getAllValues();
        assertThat(capturedKeys.get(0)).isEqualTo(capturedKeys.get(1));
    }
}
