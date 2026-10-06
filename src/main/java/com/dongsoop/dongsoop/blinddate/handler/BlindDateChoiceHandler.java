package com.dongsoop.dongsoop.blinddate.handler;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.exception.InvalidBlindDateChoiceException;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateMatchNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlindDateChoiceHandler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final BlindDateParticipantStorage participantStorage;
    private final BlindDateSessionStorage sessionStorage;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;
    private final BlindDateEventQueue eventQueue;
    private final BlindDateMatchNotification matchNotification;

    public void execute(String sessionId, Long choicerId, Long targetId) {
        eventQueue.submitChoice(sessionId, () -> handle(sessionId, choicerId, targetId));
    }

    public void timeout(String sessionId) {
        eventQueue.submitChoice(sessionId, () -> {
            if (sessionStorage.isProcessing(sessionId) && participantStorage.completeChoices(sessionId)) {
                eventQueue.closeChoices(sessionId, () -> finalizeSession(sessionId));
            }
        });
    }

    private void handle(String sessionId, Long choicerId, Long targetId) {
        if (!sessionStorage.isProcessing(sessionId)) {
            return;
        }
        try {
            if (participantStorage.recordChoice(sessionId, choicerId, targetId)) {
                // 전원 응답을 받은 한 요청만 처리 권한을 얻는다. 먼저 접수를 닫는다.
                eventQueue.closeChoices(sessionId, () -> finalizeSession(sessionId));
            }
        } catch (InvalidBlindDateChoiceException e) {
            sendResult(BlindDateTopic.choiceError(sessionId, choicerId),
                    Map.of("status", e.getStatus(), "code", e.getCode(), "message", e.getMessage()));
        }
    }

    private void finalizeSession(String sessionId) {
        try {
            if (!sessionStorage.isProcessing(sessionId)) {
                return;
            }
            Map<Long, Long> choices = participantStorage.getChoices(sessionId);
            Map<Long, String> rooms = createMatchedRooms(sessionId, choices);
            // 성공만 발행한다. 실패·마감 이벤트를 기다리지 않고 선택 UI를 종료할 수 있다.
            rooms.forEach((memberId, roomId) -> sendResult(
                    BlindDateTopic.chatRoomCreated(sessionId, memberId), Map.of("chatRoomId", roomId)));
            rooms.forEach((memberId, roomId) -> notifyMatch(sessionId, memberId, roomId));
        } finally {
            // 멱등 상태는 초기화까지 유지한다. 전송 실패가 있어도 결과를 재실행하지 않는다.
            sessionStorage.terminate(sessionId);
        }
    }

    private void notifyMatch(String sessionId, Long memberId, String roomId) {
        try {
            matchNotification.send(memberId, roomId);
        } catch (Exception e) {
            // 소켓/푸시 실패로 성공한 매칭을 뒤집거나 다른 회원의 알림을 중단하지 않는다.
            log.error("[BlindDate] Failed to notify match: sessionId={}, memberId={}, roomId={}",
                    sessionId, memberId, roomId, e);
        }
    }

    private Map<Long, String> createMatchedRooms(String sessionId, Map<Long, Long> choices) {
        Map<Long, String> rooms = new LinkedHashMap<>();
        choices.forEach((memberId, targetId) -> {
            // 각 상호 선택 쌍을 한 번만 처리한다.
            if (targetId != null && memberId < targetId && memberId.equals(choices.get(targetId))) {
                try {
                    String title = String.format("[과팅] %s", LocalDate.now(KST));
                    ChatRoom room = chatRoomService.createOneToOneChatRoom(memberId, targetId, title);
                    rooms.put(memberId, room.getRoomId());
                    rooms.put(targetId, room.getRoomId());
                } catch (Exception e) {
                    log.error("[BlindDate] Failed to create matched room: sessionId={}, memberId={}, targetId={}",
                            sessionId, memberId, targetId, e);
                }
            }
        });
        return rooms;
    }

    private void sendResult(String destination, Map<String, Object> payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            // 한 수신자 전송 실패가 다른 참가자의 결과 전송을 막지 않는다.
            log.error("[BlindDate] Failed to publish result: destination={}", destination, e);
        }
    }
}
