package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.exception.BoardNotFoundException;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.util.ChatCommonUtils;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.tutoring.repository.TutoringBoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final RedisChatRepository redisChatRepository;
    private final ChatValidator chatValidator;
    private final ChatSyncService chatSyncService;
    private final ProjectBoardRepository projectBoardRepository;
    private final StudyBoardRepository studyBoardRepository;
    private final TutoringBoardRepository tutoringBoardRepository;
    private final MarketplaceBoardRepository marketplaceBoardRepository;
    private final RedisTemplate<String, Object> redisTemplate;

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
        return filterAccessibleRooms(allRooms, userId);
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

    public void handleContactRoomLeave(String roomId, Long userId) {
        ChatRoom room = getChatRoomById(roomId);
        ChatCommonUtils.deleteContactRoomMapping(redisTemplate, roomId);
        processUserLeaveWithoutKick(room, userId);
        saveRoom(room);
    }

    private void processUserLeaveWithoutKick(ChatRoom room, Long userId) {
        room.getParticipants().remove(userId);
        room.getParticipantJoinTimes().remove(userId);
        room.updateActivity();
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

    public ChatRoom createContactChatRoom(Long userId, Long targetUserId, RecruitmentType boardType, Long boardId,
                                          String boardTitle) {
        validateRecruitmentBoard(boardType, boardId);

        String existingRoomId = ChatCommonUtils.findExistingContactRoomId(redisTemplate, userId, targetUserId, boardType, boardId);
        if (existingRoomId != null) {
            return getChatRoomById(existingRoomId);
        }

        String title = String.format("[문의] %s", boardTitle);
        ChatRoom room = createNewOneToOneRoom(userId, targetUserId, title);

        ChatCommonUtils.saveContactRoomMapping(redisTemplate, userId, targetUserId, boardType, boardId, room.getRoomId());

        return saveRoom(room);
    }

    private String buildChatRoomTitle(RecruitmentType boardType, String boardTitle) {
        String prefix = "문의";

        if (boardType == null) {
            prefix = "거래";
        }

        return String.format("[%s] %s", prefix, boardTitle);
    }

    private void validateRecruitmentBoard(RecruitmentType boardType, Long boardId) {
        if (boardType == null) {
            return;
        }

        boolean projectExists = boardType == RecruitmentType.PROJECT && projectBoardRepository.existsById(boardId);
        boolean studyExists = boardType == RecruitmentType.STUDY && studyBoardRepository.existsById(boardId);
        boolean tutoringExists = boardType == RecruitmentType.TUTORING && tutoringBoardRepository.existsById(boardId);

        boolean boardExists = projectExists || studyExists || tutoringExists;

        if (!boardExists) {
            throw new BoardNotFoundException();
        }
    }

    private ChatRoom createGroupRoom(Set<Long> participants, Long creatorId, String title) {
        return ChatRoom.createWithParticipantsAndTitle(participants, creatorId, title);
    }

    private List<ChatRoom> filterAccessibleRooms(List<ChatRoom> allRooms, Long userId) {
        return allRooms.stream()
                .filter(room -> isUserAccessibleToRoom(room, userId))
                .toList();
    }

    private boolean isUserAccessibleToRoom(ChatRoom room, Long userId) {
        boolean isKicked = room.isKicked(userId);
        boolean isParticipant = room.getParticipants().contains(userId);

        return !isKicked && isParticipant;
    }

    private void updateRoomTitleIfNeeded(ChatRoom existingRoom, String requestedTitle) {
        boolean hasRequestedTitle = !ChatCommonUtils.isEmpty(requestedTitle);
        boolean currentTitleEmpty = ChatCommonUtils.isEmpty(existingRoom.getTitle());

        if (hasRequestedTitle && currentTitleEmpty) {
            existingRoom.setTitle(requestedTitle);
        }
    }
}