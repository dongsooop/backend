package com.dongsoop.dongsoop.blinddate.handler;

import com.dongsoop.dongsoop.blinddate.dto.BlindDateJoinResult;
import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo;
import com.dongsoop.dongsoop.blinddate.exception.SessionTerminatedException;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorage;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateSessionScheduler;
import com.dongsoop.dongsoop.blinddate.service.BlindDateService;
import com.dongsoop.dongsoop.blinddate.service.BlindDateSessionService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlindDateConnectHandler {

    private final BlindDateParticipantStorage participantStorage;
    private final BlindDateStorage blindDateStorage;
    private final BlindDateSessionStorage sessionStorage;
    private final BlindDateService blindDateService;
    private final BlindDateSessionService sessionService;
    private final BlindDateSessionScheduler sessionScheduler;
    private final SimpMessagingTemplate messagingTemplate;
    private final BlindDateEventQueue eventQueue;

    /**
     * 세션 참여 및 세션 id 반환
     *
     * @param socketId 참여자 소켓 id
     * @param memberId 참여자 id
     */
    public void execute(String socketId, Long memberId, Map<String, Object> sessionAttributes) {
        // 과팅 운영 중이 아닌 경우 종료
        this.validateBlindDateAvailability();

        // 참가자/세션 상태를 건드리는 처리는 전부 큐에서 순서대로 처리
        eventQueue.submit(() -> handle(socketId, memberId, sessionAttributes));
    }

    private void handle(String socketId, Long memberId, Map<String, Object> sessionAttributes) {
        // 이미 참여 중인 경우 소켓만 추가 후 종료
        String existingSessionId;
        try {
            existingSessionId = this.tryHandleReconnection(socketId, memberId);
        } catch (SessionTerminatedException e) {
            // 재접속하려는 세션이 이미 종료된 경우: 죽은 참가자 기록을 정리하고 클라이언트에 알림
            log.info("[BlindDate] Reconnect target session already terminated: memberId={}", memberId);
            participantStorage.removeParticipant(memberId);
            sessionAttributes.remove("sessionId");
            sendSessionTerminatedEvent(memberId);
            return;
        }

        if (existingSessionId != null) {
            sessionAttributes.put("sessionId", existingSessionId);
            return;
        }

        // 사용자 입장 처리
        BlindDateJoinResult joinResult = this.join(socketId, memberId, sessionAttributes);

        // 입장되지 않은 경우 종료
        if (joinResult == null) {
            return;
        }

        String sessionId = joinResult.sessionId();

        // 마지막 참여자인지 검증 후 과팅 세션 시작 시도
        if (tryStart(sessionId)) {
            // 마지막으로 입장한 사용자의 소켓 수신을 위해 현재 스레드를 종료하고 새 스레드에서 처리
            new Thread(() -> sessionScheduler.start(sessionId)).start();
            return;
        }

        // 마지막 참여자가 아닌 경우 인원 업데이트 브로드캐스트
        blindDateService.broadcastJoinedCount(joinResult.sessionId(), joinResult.currentCount());
    }

    private boolean tryStart(String sessionId) {
        // 마지막 참여자인 경우 세션 시작
        if (sessionService.isSessionFull(sessionId)) {
            if (!sessionStorage.isWaiting(sessionId)) {
                log.warn("[BlindDate] Is last member by session {}, but not waiting", sessionId);
                return false;
            }

            // 세션 상태 변경 - PROCESSING
            sessionStorage.start(sessionId);
            return true;
        }

        return false;
    }

    private BlindDateJoinResult join(String socketId, Long memberId, Map<String, Object> sessionAttributes) {
        BlindDateJoinResult joinResult;

        try {
            // 과팅 세션 할당 (Pointer 기반, 큐에서 순서대로 처리되므로 동시성 보장)
            String sessionId = assignSession();

            // 과팅 세션 id 세션 속성에 저장
            sessionAttributes.put("sessionId", sessionId);

            // 참여 정보 추가 (assignSession에서 편입 가능한 과팅 세션 여부를 확인했기에 바로 저장)
            ParticipantInfo participant = participantStorage.addParticipant(sessionId, memberId, socketId);

            // 과팅 세션 편입 후 참가자 수 조회
            List<ParticipantInfo> participantInfos = participantStorage.findAllBySessionId(sessionId);
            int currentCount = participantInfos.size();
            int maxCount = blindDateStorage.getMaxSessionMemberCount();

            joinResult = new BlindDateJoinResult(participant, sessionId, currentCount, maxCount);
        } catch (Exception e) {
            // addParticipant는 compute() 기반이라 예외 발생 시 참가자 맵에 아무 것도 반영되지 않는다.
            // 따라서 여기서 회원을 제거하면, 다른 세션에 이미 정상 등록된 참가자 정보를 잘못 지울 수 있다.
            log.error("[BlindDate] Exception from enter process: memberId={}", memberId, e);
            sessionAttributes.remove("sessionId");

            return null;
        }

        try {
            // 입장한 사용자에게 정보 전달
            sendJoinEvent(joinResult);
        } catch (Exception e) {
            // 입장한 사용자에게 정보 전달 실패 시 소켓 연결 해지로 보고 Disconnect에서 처리하도록 종료
            log.info("[BlindDate] Failed to send JOIN event, rolling back participant: memberId={}", memberId, e);
            return null;
        }

        return joinResult;
    }

    /**
     * 과팅 개최 여부 검증
     */
    private void validateBlindDateAvailability() {
        if (!blindDateService.isAvailable()) {
            throw new IllegalStateException("현재 과팅이 운영되지 않습니다.");
        }
    }

    /**
     * 이미 매칭된 사용자의 요청인 경우 소켓만 추가 처리
     *
     * @param socketId 소켓 id
     * @param memberId 회원 id
     * @return 기존 세션 ID (재연결인 경우), null (첫 연결인 경우)
     */
    private String tryHandleReconnection(String socketId, Long memberId) {
        ParticipantInfo existingParticipant = participantStorage.getByMemberId(memberId);
        // 첫 매칭인 경우
        if (existingParticipant == null) {
            return null;
        }

        String existingSessionId = existingParticipant.getSessionId();

        // 재연결된 세션이 존재하지 않는 경우 (세션 종료 후 재연결 시도 등) 예외 처리
        if (this.sessionStorage.getState(existingSessionId) == null) {
            throw new SessionTerminatedException();
        }

        // addParticipant는 같은 세션이면 소켓 추가 + socketId->memberId 역인덱스 갱신까지 함께 처리한다.
        // existingParticipant.addSocket()을 직접 호출하면 역인덱스가 안 갱신되어, 이 소켓이
        // 나중에 연결을 끊어도 removeSocket()이 찾지 못해 참가자가 정리되지 않는 문제가 있었다.
        participantStorage.addParticipant(existingSessionId, memberId, socketId);
        return existingSessionId;
    }

    /**
     * 세션 할당 (큐에서 순서대로 처리되어 동시성 보장)
     *
     * @return 할당된 세션 ID
     */
    private String assignSession() {
        // Pointer 조회
        String pointer = blindDateStorage.getPointer();

        // Pointer가 없는 경우 새 세션 생성
        if (pointer == null) {
            SessionInfo newSession = this.sessionStorage.create();
            String newSessionId = newSession.getSessionId();

            blindDateStorage.setPointer(newSessionId);

            return newSessionId;
        }

        // Pointer 세션이 꽉 찬 경우 새 세션 생성
        if (sessionService.isSessionFull(pointer)) {
            SessionInfo newSession = this.sessionStorage.create();
            String newSessionId = newSession.getSessionId();

            blindDateStorage.setPointer(newSessionId);

            return newSessionId;
        }

        return pointer;
    }

    /**
     * 입장 이벤트 전송
     *
     * @param joinResult 입장 결과 (참여 정보, 참여자 수, 세션 id, 최대 수용 인원)
     */
    private void sendJoinEvent(BlindDateJoinResult joinResult) {
        ParticipantInfo participantInfo = joinResult.participantInfo();
        Map<String, Object> event = Map.of(
                "name", participantInfo.getAnonymousName(),
                "sessionId", joinResult.sessionId(),
                "state", "WAITING",
                "volunteer", joinResult.currentCount()
        );

        String destination = "/queue/blinddate/join";

        try {
            messagingTemplate.convertAndSendToUser(
                    participantInfo.getMemberId().toString(),
                    destination,
                    event
            );
        } catch (Exception e) {
            log.error("Failed to send JOIN event: memberId={}", participantInfo.getMemberId(), e);
            throw e;
        }
    }

    /**
     * 재접속하려는 세션이 이미 종료되었음을 클라이언트에 알림
     *
     * @param memberId 알림 대상 회원 id
     */
    private void sendSessionTerminatedEvent(Long memberId) {
        Map<String, Object> event = Map.of("state", "TERMINATED");

        try {
            messagingTemplate.convertAndSendToUser(
                    memberId.toString(),
                    "/queue/blinddate/join",
                    event
            );
        } catch (Exception e) {
            log.error("Failed to send SESSION_TERMINATED event: memberId={}", memberId, e);
        }
    }
}
