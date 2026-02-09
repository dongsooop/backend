package com.dongsoop.dongsoop.blinddate.repository;

import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BlindDateParticipantStorage {

    /**
     * 참여자 추가 또는 소켓 추가
     *
     * @param sessionId 참여하려는 세션 id
     * @param memberId  참여 주체 회원 id
     * @param socketId  참여 주체 소켓 id
     */
    ParticipantInfo addParticipant(String sessionId, Long memberId, String socketId);

    /**
     * 소켓 제거 (연결 해제) 모든 소켓이 제거되면 참여자도 제거
     */
    boolean removeSocket(String socketId);

    /**
     * 회원 ID로 참여 정보 조회
     */
    ParticipantInfo getByMemberId(Long memberId);

    /**
     * 소켓 ID로 참여 정보 조회
     */
    ParticipantInfo getBySocketId(String socketId);

    /**
     * 세션의 참여자 수 조회
     */
    List<ParticipantInfo> findAllBySessionId(String sessionId);

    /**
     * 세션의 참여자 ID와 이름 Map
     */
    Map<Long, String> getParticipantsIdAndName(String sessionId);

    /**
     * 선택 기록
     */
    boolean recordChoice(String sessionId, Long choicerId, Long targetId);

    /**
     * 매칭 확인
     */
    boolean isMatched(String sessionId, Long memberId);

    /**
     * 전체 데이터 초기화
     */
    void clear();

    /**
     * 참여자 제거 (회원 ID로)
     */
    void removeParticipant(Long memberId);

    /**
     * 익명 이름 조회
     */
    String getAnonymousName(Long memberId);

    /**
     * 매칭되지 않은 멤버 조회
     */
    Set<Long> getNotMatched(String sessionId);
}
