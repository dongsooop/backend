package com.dongsoop.dongsoop.blinddate.entity;

/**
 * 블라인드 데이트 세션 상태
 */
public enum SessionStatus {
    WAITING,    // 대기 중
    MATCHED,    // 매칭됨
    CHATTING,   // 채팅 중
    COMPLETED,  // 완료
    DISCONNECTED // 연결 해제
}
