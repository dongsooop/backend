package com.dongsoop.dongsoop.blinddate.handler;

import com.dongsoop.dongsoop.blinddate.dto.BlindDateJoinResult;
import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateMatchingLock;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateMemberLock;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateInfoRepositoryImpl;
import com.dongsoop.dongsoop.blinddate.repository.ParticipantInfoRepository;
import com.dongsoop.dongsoop.blinddate.repository.SessionInfoRepository;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateSessionScheduler;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateTaskScheduler;
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

    private final ParticipantInfoRepository participantInfoRepository;
    private final BlindDateInfoRepositoryImpl blindDateInfoRepository;
    private final SessionInfoRepository sessionInfoRepository;
    private final BlindDateService blindDateService;
    private final BlindDateSessionService sessionService;
    private final BlindDateSessionScheduler sessionScheduler;
    private final BlindDateTaskScheduler blindDateTaskScheduler;
    private final SimpMessagingTemplate messagingTemplate;
    private final BlindDateMatchingLock blindDateMatchingLock;
    private final BlindDateMemberLock blindDateMemberLock;

    /**
     * 세션 참여 및 세션 id 반환
     *
     * @param socketId 참여자 소켓 id
     * @param memberId 참여자 id
     */
    public void execute(String socketId, Long memberId, Map<String, Object> sessionAttributes) {
        // 과팅 운영 중이 아닌 경우 종료
        this.validateBlindDateAvailability();

        // 이미 참여 중인 경우 소켓만 추가 후 종료
        String existingSessionId = this.tryHandleReconnection(socketId, memberId);
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

        this.blindDateTaskScheduler.execute(() -> {
            // 마지막 참여자인지 검증 후 과팅 세션 시작 시도
            if (!tryStartSession(joinResult.sessionId())) {
                // 마지막 참여자가 아닌 경우 인원 업데이트 브로드캐스트
                blindDateService.broadcastJoinedCount(joinResult.sessionId(), joinResult.currentCount());
            }
        });
    }

    private boolean tryStartSession(String sessionId) {
        // 마지막 참여자인 경우 세션 시작
        if (sessionService.isSessionFull(sessionId)) {
            sessionScheduler.start(sessionId);
            return true;
        }

        return false;
    }

    private BlindDateJoinResult join(String socketId, Long memberId, Map<String, Object> sessionAttributes) {
        // 처음 입장 시 포인터 할당을 위해 매칭 획득
        blindDateMatchingLock.lock();

        try {
            // 과팅 세션 할당 (Pointer 기반, Lock으로 동시성 보장)
            String sessionId = assignSession();

            // 과팅 세션 id 세션 속성에 저장
            sessionAttributes.put("sessionId", sessionId);

            // 참여 정보 추가 (assignSession에서 편입 가능한 과팅 세션 여부를 확인했기에 바로 저장)
            ParticipantInfo participant = participantInfoRepository.addParticipant(sessionId, memberId, socketId);

            // 과팅 세션 편입 후 참가자 수 조회
            List<ParticipantInfo> participantInfos = participantInfoRepository.findAllBySessionId(sessionId);
            int currentCount = participantInfos.size();
            int maxCount = blindDateInfoRepository.getMaxSessionMemberCount();

            BlindDateJoinResult joinResult = new BlindDateJoinResult(participant, sessionId, currentCount, maxCount);

            // 입장한 사용자에게 정보 전달
            sendJoinEvent(joinResult);

            return joinResult;
        } catch (Exception e) {
            // 입장 과정에서 오류 발생 시 회원 제거
            this.participantInfoRepository.removeParticipant(memberId);
            sessionAttributes.remove("sessionId");

            return null;
        } finally {
            // 회원 편입 후 회원 락 해제
            blindDateMatchingLock.unlock();
        }
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
        // 이미 참여중인 시나리오에 대해 안전한 소켓 추가를 위해 회원 락 획득
        blindDateMemberLock.lockByMemberId(memberId);

        try {
            ParticipantInfo existingParticipant = participantInfoRepository.getByMemberId(memberId);
            // 첫 매칭인 경우
            if (existingParticipant == null) {
                return null;
            }

            String existingSessionId = existingParticipant.getSessionId();

            // 재연결된 세션이 존재하지 않는 경우 (세션 종료 후 재연결 시도 등) 예외 처리
            if (this.sessionInfoRepository.getState(existingSessionId) == null) {
                throw new IllegalArgumentException("재연결된 세션 정보가 존재하지 않습니다.");
            }

            existingParticipant.addSocket(socketId);
            return existingSessionId;
        } finally {
            blindDateMemberLock.unlockByMemberId(memberId); // 회원 락 해제
        }
    }

    /**
     * 세션 할당 (Lock으로 동시성 보장)
     *
     * @return 할당된 세션 ID
     */
    private String assignSession() {
        // Pointer 조회
        String pointer = blindDateInfoRepository.getPointer();

        // Pointer가 없는 경우 새 세션 생성
        if (pointer == null) {
            SessionInfo newSession = sessionInfoRepository.create();
            String newSessionId = newSession.getSessionId();

            blindDateInfoRepository.setPointer(newSessionId);

            return newSessionId;
        }

        // Pointer 세션이 꽉 찬 경우 새 세션 생성
        if (sessionService.isSessionFull(pointer)) {
            SessionInfo newSession = sessionInfoRepository.create();
            String newSessionId = newSession.getSessionId();

            blindDateInfoRepository.setPointer(newSessionId);

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
}
