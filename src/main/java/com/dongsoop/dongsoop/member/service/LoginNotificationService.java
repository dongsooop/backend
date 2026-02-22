package com.dongsoop.dongsoop.member.service;

/**
 * 로그인 알림 서비스 인터페이스.
 *
 * <p>로그인 감지 시 이메일 및 FCM 푸시 알림을 발송한다.
 */
public interface LoginNotificationService {

    /**
     * 로그인 알림을 발송한다.
     *
     * <p>이메일 알림과 해당 회원의 모든 등록 기기에 FCM 푸시 알림을 동시에 발송한다.
     *
     * @param memberId 로그인한 회원의 ID
     * @param email    알림을 수신할 이메일 주소
     */
    void sendLoginNotification(Long memberId, String email);
}
