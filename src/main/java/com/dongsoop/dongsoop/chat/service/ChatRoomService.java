package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.util.ChatCommonUtils;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final RedisChatRepository redisChatRepository;
    private final ChatValidator chatValidator;
    private final ChatSyncService chatSyncService;

    public ChatRoom createOneToOneChatRoom(Long userId, Long targetUserId, String title) {
        validateOneToOneChatCreation(userId, targetUserId);
        return findExistingRoomOrCreate(userId, targetUserId, title);
    }

    public ChatRoom createGroupChatRoom(Long creatorId, Set<Long> participants, String title) {
        validateGroupChatCreation(participants);

        ChatRoom room = createGroupRoom(participants, creatorId, title);
        return saveRoom(room);
    }

    public ChatRoom getChatRoomById(String roomId) {
        return chatSyncService.findRoomOrRestore(roomId);
    }

    public List<ChatRoom> getRoomsForUserId(Long userId) {
        List<ChatRoom> allRooms = redisChatRepository.findRoomsByUserId(userId);
        return filterNotKickedRooms(allRooms, userId);
    }

    public void enterChatRoom(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);
    }

    public ChatRoom saveRoom(ChatRoom room) {
        return redisChatRepository.saveRoom(room);
    }

    public void deleteRoom(String roomId) {
        redisChatRepository.deleteRoom(roomId);
    }

    public ChatRoom updateRoomActivity(String roomId) {
        ChatRoom room = getChatRoomById(roomId);
        room.updateActivity();
        return saveRoom(room);
    }

    public void deleteRoomIfEmpty(ChatRoom room) {
        boolean roomEmpty = room.getParticipants().isEmpty();
        if (roomEmpty) {
            deleteRoom(room.getRoomId());
        }
    }

    private void validateOneToOneChatCreation(Long userId, Long targetUserId) {
        chatValidator.validateSelfChat(userId, targetUserId);
        ChatCommonUtils.validatePositiveUserId(userId);
        ChatCommonUtils.validatePositiveUserId(targetUserId);
    }

    private void validateGroupChatCreation(Set<Long> participants) {
        boolean participantsEmpty = participants.isEmpty();
        if (participantsEmpty) {
            throw new IllegalArgumentException("그룹 채팅 참여자 수가 올바르지 않습니다.");
        }
    }

    private ChatRoom findExistingRoomOrCreate(Long userId, Long targetUserId, String title) {
        ChatRoom existingRoom = redisChatRepository.findRoomByParticipants(userId, targetUserId).orElse(null);

        boolean roomExists = existingRoom != null;
        if (roomExists) {
            updateRoomTitleIfNeeded(existingRoom, title);
            return saveRoom(existingRoom);
        }
        return createNewOneToOneRoom(userId, targetUserId, title);
    }

    private ChatRoom createNewOneToOneRoom(Long userId, Long targetUserId, String title) {
        ChatRoom room = ChatRoom.create(userId, targetUserId, title);
        return saveRoom(room);
    }

    private ChatRoom createGroupRoom(Set<Long> participants, Long creatorId, String title) {
        return ChatRoom.createWithParticipantsAndTitle(participants, creatorId, title);
    }

    private List<ChatRoom> filterNotKickedRooms(List<ChatRoom> allRooms, Long userId) {
        return allRooms.stream()
                .filter(room -> !room.isKicked(userId))
                .toList();
    }

    private void updateRoomTitleIfNeeded(ChatRoom existingRoom, String requestedTitle) {
        boolean hasRequestedTitle = !ChatCommonUtils.isEmpty(requestedTitle);
        boolean currentTitleEmpty = ChatCommonUtils.isEmpty(existingRoom.getTitle());

        if (hasRequestedTitle && currentTitleEmpty) {
            existingRoom.setTitle(requestedTitle);
        }
    }
}
