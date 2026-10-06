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
     * 소켓 제거 (연결 해제)
     *
     * @return 제거 후 연결된 소켓이 하나도 없으면 true
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

    /** 선택 대상 참가자 목록을 고정한다. 반복 호출로 기존 응답을 초기화하지 않는다. */
    void openChoices(String sessionId);

    /** 전원 응답 후 확정된 선택을 반환한다. null은 미선택이다. */
    Map<Long, Long> getChoices(String sessionId);

    /**
     * 최초 유효 응답을 기록한다. 전원 응답 후 처리 권한을 획득한 한 호출만 true를 반환한다.
     */
    boolean recordChoice(String sessionId, Long choicerId, Long targetId);

    /** 미응답을 미선택으로 채우고 처리 권한을 한 번만 획득한다. */
    boolean completeChoices(String sessionId);

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
