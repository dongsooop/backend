package com.dongsoop.dongsoop.blinddate.handler;

import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateMatchingLock;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateMemberLock;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import com.dongsoop.dongsoop.blinddate.service.BlindDateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlindDateDisconnectHandler {

    private final BlindDateParticipantStorage participantStorage;
    private final BlindDateSessionStorage sessionStorage;
    private final BlindDateService blindDateService;
    private final BlindDateMatchingLock matchingLock;
    private final BlindDateMemberLock memberLock;

    public void execute(String socketId, Long memberId, String sessionId) {
        // 종료된 세션에서 나가는 경우 별도 처리 안 함
        if (this.sessionStorage.getState(sessionId) == null) {
            log.info("Session ID {} has been deleted", sessionId);
            return;
        }

        // 참여자 정보에서 소켓 제거 시 남아있는 소켓이 없는지
        boolean isExit = this.removeSocketByParticipantInfo(socketId, memberId);

        // 사용자의 연결된 소켓이 없는 경우 퇴장 처리
        if (isExit) {
            // 참여중인 세션이 포인터 세션인 경우 회원도 제거 시도
            this.tryRemoveMember(memberId, sessionId);
        }
    }

    private boolean removeSocketByParticipantInfo(String socketId, Long memberId) {
        // 소켓 제거 중 회원 소켓이 추가되지 않도록 처리
        memberLock.lockByMemberId(memberId);

        try {
            // 소켓만 제거 (모든 소켓이 제거되면 자동으로 참여자도 제거됨)
            // 제거 후 모든 소켓을 제거했는지 여부 반환
            return participantStorage.removeSocket(socketId);
        } catch (IllegalArgumentException e) {
            log.warn("Socket already removed or not found: socketId={}, memberId={}", socketId, memberId);
            return false;
        } finally {
            // 락 해제
            memberLock.unlockByMemberId(memberId);
        }
    }

    private void tryRemoveMember(Long memberId, String sessionId) {
        // 퇴장하려는 세션이 포인터 세션일 수 있기에 매칭 락
        matchingLock.lock();

        try {
            // 세션 상태 확인
            SessionState state = sessionStorage.getState(sessionId);

            // PROCESSING 상태면 포인터가 아니며, 퇴장 처리 안 함
            if (state != SessionState.WAITING) {
                return;
            }

            // WAITING 상태일 때만 퇴장 처리
            participantStorage.removeParticipant(memberId);

            List<ParticipantInfo> participantInfos = participantStorage.findAllBySessionId(sessionId);

            // 인원 업데이트 브로드캐스트
            blindDateService.broadcastJoinedCount(sessionId, participantInfos.size());
        } finally {
            // 락 해제
            matchingLock.unlock();
        }
    }
}
