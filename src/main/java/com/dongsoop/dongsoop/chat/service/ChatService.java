package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.ChatRepository;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.exception.domain.websocket.ChatRoomNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatValidator chatValidator;
    private final ChatSyncService chatSyncService;

    public ChatService(@Qualifier("redisChatRepository") ChatRepository chatRepository,
                       ChatValidator chatValidator,
                       ChatSyncService chatSyncService) {
        this.chatRepository = chatRepository;
        this.chatValidator = chatValidator;
        this.chatSyncService = chatSyncService;
    }

    public ChatRoom createOneToOneChatRoom(String userId, String targetUserId) {
        chatValidator.validateSelfChat(userId, targetUserId);
        return chatRepository.findRoomByParticipants(userId, targetUserId)
                .orElseGet(() -> createRoom(userId, targetUserId));
    }

    public ChatRoom createGroupChatRoom(String creatorId, Set<String> participants) {
        if (participants.size() < 2) {
            throw new IllegalArgumentException("그룹 채팅에는 최소 2명 이상의 참여자가 필요합니다.");
        }

        // 생성자도 참여자에 포함
        participants.add(creatorId);

        String roomId = UUID.randomUUID().toString();
        ChatRoom room = ChatRoom.builder()
                .roomId(roomId)
                .participants(participants)
                .build();

        return chatRepository.saveRoom(room);
    }

    public void enterChatRoom(String roomId, String userId) {
        chatValidator.validateUserForRoom(roomId, userId);
    }

    public ChatMessage processMessage(ChatMessage message) {
        ChatMessage validatedMessage = chatValidator.validateAndEnrichMessage(message);
        chatRepository.saveMessage(validatedMessage);
        return validatedMessage;
    }

    public ChatMessage createEnterMessage(String roomId, String userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        ChatMessage message = ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId(userId)
                .content(userId + "님이 입장하셨습니다.")
                .timestamp(LocalDateTime.now())
                .type(MessageType.ENTER)
                .build();

        chatRepository.saveMessage(message);
        return message;
    }

    public List<ChatMessage> getChatHistory(String roomId, String userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        List<ChatMessage> messages = chatRepository.findMessagesByRoomId(roomId);

        if (messages.isEmpty()) {
            log.info("Redis에 메시지 없음, DB에서 복구 시도: roomId={}", roomId);
            messages = chatSyncService.restoreMessagesFromDatabase(roomId);
        }

        return messages;
    }

    public ChatRoom getChatRoomById(String roomId) {
        return chatRepository.findRoomById(roomId)
                .orElseGet(() -> {
                    // Redis에 없는 경우 PostgreSQL에서 복원 시도
                    ChatRoom restoredRoom = chatSyncService.restoreGroupChatRoom(roomId);
                    if (restoredRoom == null) {
                        throw new ChatRoomNotFoundException();
                    }
                    return restoredRoom;
                });
    }

    public List<ChatMessage> syncMessages(String roomId, String userId, List<ChatMessage> clientMessages) {
        chatValidator.validateUserForRoom(roomId, userId);

        // 서버에 있는 메시지 가져오기
        List<ChatMessage> serverMessages = chatRepository.findMessagesByRoomId(roomId);

        // 클라이언트에서 온 메시지 중 서버에 없는 메시지만 필터링
        List<ChatMessage> newMessages = chatValidator.filterDuplicateMessages(serverMessages, clientMessages);

        // 새 메시지 저장
        for (ChatMessage message : newMessages) {
            ChatMessage validatedMessage = chatValidator.validateAndEnrichMessage(message);
            chatRepository.saveMessage(validatedMessage);
        }

        return chatRepository.findMessagesByRoomId(roomId);
    }

    public void recreateRoomIfNeeded(String roomId, String userId, List<ChatMessage> localMessages) {
        try {
            // 채팅방이 존재하는지 확인
            ChatRoom room = getChatRoomById(roomId);

            // 사용자가 채팅방에 있는지 확인
            if (!room.getParticipants().contains(userId)) {
                room.getParticipants().add(userId);
                chatRepository.saveRoom(room);
            }

            // 동기화 처리
            syncMessages(roomId, userId, localMessages);
        } catch (ChatRoomNotFoundException e) {
            // 채팅방이 없으면 로컬 스토리지 메시지로 재생성
            if (!localMessages.isEmpty()) {
                recreateRoomFromMessages(roomId, userId, localMessages);
                log.info("채팅방 {} 재생성 완료: 사용자 {}", roomId, userId);
            } else {
                throw e;
            }
        }
    }

    public ChatRoom recreateRoomFromMessages(String roomId, String userId, List<ChatMessage> clientMessages) {
        if (clientMessages.isEmpty()) {
            throw new IllegalArgumentException("메시지가 없어 채팅방을 재생성할 수 없습니다.");
        }

        Set<String> participants = extractParticipantsFromMessages(userId, clientMessages);

        // 채팅방 생성
        ChatRoom newRoom = ChatRoom.builder()
                .roomId(roomId)
                .participants(participants)
                .build();

        chatRepository.saveRoom(newRoom);

        // 메시지 저장
        for (ChatMessage message : clientMessages) {
            // 기본 정보 설정
            if (message.getMessageId() == null) {
                message.setMessageId(UUID.randomUUID().toString());
            }
            if (message.getTimestamp() == null) {
                message.setTimestamp(LocalDateTime.now());
            }
            if (message.getType() == null) {
                message.setType(MessageType.CHAT);
            }

            chatRepository.saveMessage(message);
        }

        return newRoom;
    }

    private Set<String> extractParticipantsFromMessages(String userId, List<ChatMessage> messages) {
        Set<String> participants = new HashSet<>();
        participants.add(userId);

        for (ChatMessage message : messages) {
            participants.add(message.getSenderId());
        }

        return participants;
    }

    private ChatRoom createRoom(String user1, String user2) {
        ChatRoom room = ChatRoom.create(user1, user2);
        return chatRepository.saveRoom(room);
    }

    public List<ChatRoom> findRoomsByUserId(String userId) {
        return chatRepository.findRoomsByUserId(userId);
    }

    public List<ChatRoom> getRoomsForUserId(String userId) {
        return findRoomsByUserId(userId);
    }
}