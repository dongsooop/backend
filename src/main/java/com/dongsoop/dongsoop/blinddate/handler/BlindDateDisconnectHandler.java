package com.dongsoop.dongsoop.blinddate.handler;

import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateMatchingLock;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateMemberLock;
import com.dongsoop.dongsoop.blinddate.repository.ParticipantInfoRepository;
import com.dongsoop.dongsoop.blinddate.repository.SessionInfoRepository;
import com.dongsoop.dongsoop.blinddate.service.BlindDateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlindDateDisconnectHandler {

    private final ParticipantInfoRepository participantInfoRepository;
    private final SessionInfoRepository sessionInfoRepository;
    private final BlindDateService blindDateService;
    private final BlindDateMatchingLock matchingLock;
    private final BlindDateMemberLock memberLock;

    public void execute(String socketId, Long memberId, String sessionId) {
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
            boolean fullyRemoved = participantInfoRepository.removeSocket(socketId);

            // 전체를 삭제한 경우 true 반환
            return fullyRemoved;
        } finally {
            // 락 해제
            memberLock.unlockByMemberId(memberId);
        }
    }

    private void tryRemoveMember(Long memberId, String sessionId) {
        // 퇴장하려는 세션이 포인터 세션일 수 있기에 매칭 락
        matchingLock.lock();

        // 세션 상태 확인
        SessionState state = sessionInfoRepository.getState(sessionId);

        // PROCESSING 상태면 포인터가 아니며, 퇴장 처리 안 함
        if (state != SessionState.WAITING) {
            // 이미 진행중이거나 종료된 세션인 경우 락 해제
            matchingLock.unlock();
            return;
        }

        // WAITING 상태일 때만 퇴장 처리
        participantInfoRepository.removeParticipant(memberId);

        List<ParticipantInfo> participantInfos = participantInfoRepository.findAllBySessionId(sessionId);

        // 인원 업데이트 브로드캐스트
        blindDateService.broadcastJoinedCount(sessionId, participantInfos.size());

        // 인원 업데이트 후 매칭 락 해제
        matchingLock.unlock();
    }
}
