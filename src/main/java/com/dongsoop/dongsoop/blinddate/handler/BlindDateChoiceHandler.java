package com.dongsoop.dongsoop.blinddate.handler;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlindDateChoiceHandler {

    private final BlindDateParticipantStorage participantStorage;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;

    public void execute(String sessionId, Long choicerId, Long targetId) {
        // 선택 및 매칭 확인
        boolean isMatched = participantStorage.recordChoice(sessionId, choicerId, targetId);

        if (isMatched) {
            String chatRoomTitle = LocalDateTime.now().toString();
            ChatRoom chatRoom = chatRoomService.createOneToOneChatRoom(choicerId, targetId, chatRoomTitle);

            // 양쪽에 채팅방 ID 전송
            sendChatRoomCreated(sessionId, choicerId, chatRoom.getRoomId());
            sendChatRoomCreated(sessionId, targetId, chatRoom.getRoomId());
        }
    }

    /**
     * 채팅방 생성 이벤트 전송
     *
     * @param sessionId  과팅 세션 id
     * @param memberId   수신자 id
     * @param chatRoomId 개설된 채팅방 id
     */
    private void sendChatRoomCreated(String sessionId, Long memberId, String chatRoomId) {
        messagingTemplate.convertAndSend(
                BlindDateTopic.chatRoomCreated(sessionId, memberId),
                Map.of("chatRoomId", chatRoomId)
        );
    }
}
