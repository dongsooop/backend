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
        // 새 접수는 즉시 차단하고 앞서 접수된 작업 뒤에서 상태만 종료한다.
        eventQueue.closeChoices(sessionId, () -> sessionStorage.terminate(sessionId));
    }

    private void handle(String sessionId, Long choicerId, Long targetId) {
        if (!sessionStorage.isProcessing(sessionId)) {
            return;
        }
        try {
            if (participantStorage.recordChoice(sessionId, choicerId, targetId)) {
                Long mutualTargetId = participantStorage.claimMutualChoice(sessionId, choicerId);
                if (mutualTargetId != null) {
                    createMatch(sessionId, choicerId, mutualTargetId);
                }
            }
        } catch (InvalidBlindDateChoiceException e) {
            sendResult(BlindDateTopic.choiceError(sessionId, choicerId),
                    Map.of("status", e.getStatus(), "code", e.getCode(), "message", e.getMessage()));
        }
    }

    private void createMatch(String sessionId, Long memberId, Long targetId) {
        // 종료된 세션은 새로운 매칭을 만들지 않는다.
        if (!sessionStorage.isProcessing(sessionId)) {
            return;
        }
        String roomId;
        try {
            String title = String.format("[과팅] %s", LocalDate.now(KST));
            ChatRoom room = chatRoomService.createOneToOneChatRoom(
                    Math.min(memberId, targetId), Math.max(memberId, targetId), title);
            roomId = room.getRoomId();
        } catch (Exception e) {
            log.error("[BlindDate] Failed to create matched room: sessionId={}, memberId={}, targetId={}",
                    sessionId, memberId, targetId, e);
            return;
        }
        sendResult(BlindDateTopic.chatRoomCreated(sessionId, memberId), Map.of("chatRoomId", roomId));
        sendResult(BlindDateTopic.chatRoomCreated(sessionId, targetId), Map.of("chatRoomId", roomId));
        notifyMatch(sessionId, memberId, roomId);
        notifyMatch(sessionId, targetId, roomId);
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

    private void sendResult(String destination, Map<String, Object> payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            // 한 수신자 전송 실패가 다른 참가자의 결과 전송을 막지 않는다.
            log.error("[BlindDate] Failed to publish result: destination={}", destination, e);
        }
    }
}
