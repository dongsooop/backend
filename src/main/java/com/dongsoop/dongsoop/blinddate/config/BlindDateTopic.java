package com.dongsoop.dongsoop.blinddate.config;

/**
 * 과팅 메시지 토픽 생성 유틸
 */
public class BlindDateTopic {

    private static final String BASE = "/topic/blinddate";
    private static final String SESSION_BASE = BASE + "/session";

    /**
     * 세션별 이벤트 토픽
     */
    public static String sessionEvent(String sessionId, String eventType) {
        return SESSION_BASE + "/" + sessionId + "/" + eventType;
    }

    /**
     * 세션 시작 토픽
     */
    public static String sessionStart(String sessionId) {
        return sessionEvent(sessionId, "start");
    }

    /**
     * 채팅 비활성화 토픽
     */
    public static String freeze(String sessionId) {
        return sessionEvent(sessionId, "freeze");
    }

    /**
     * 채팅 활성화 토픽
     */
    public static String thaw(String sessionId) {
        return sessionEvent(sessionId, "thaw");
    }

    /**
     * 시스템 메시지 토픽
     */
    public static String system(String sessionId) {
        return sessionEvent(sessionId, "system");
    }

    /**
     * 채팅 메시지 토픽
     */
    public static String message(String sessionId) {
        return sessionEvent(sessionId, "message");
    }

    /**
     * 인원 업데이트 토픽
     */
    public static String joined(String sessionId) {
        return sessionEvent(sessionId, "joined");
    }

    /**
     * 참가자 입장 토픽
     */
    public static String join() {
        return BASE + "/join";
    }

    /**
     * 참가자 목록 토픽 (사랑의 작대기용)
     */
    public static String participants(String sessionId) {
        return sessionEvent(sessionId, "participants");
    }

    /**
     * 개인별 이벤트 토픽
     */
    public static String memberEvent(String sessionId, Long memberId, String eventType) {
        return SESSION_BASE + "/" + sessionId + "/member/" + memberId + "/" + eventType;
    }

    /**
     * 채팅방 생성 알림 (매칭 성공)
     */
    public static String chatRoomCreated(String sessionId, Long memberId) {
        return memberEvent(sessionId, memberId, "chatroom");
    }

    /**
     * 매칭 실패 알림
     */
    public static String matchFailed(String sessionId, Long memberId) {
        return memberEvent(sessionId, memberId, "failed");
    }
}
