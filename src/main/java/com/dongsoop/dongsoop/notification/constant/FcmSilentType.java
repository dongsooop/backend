package com.dongsoop.dongsoop.notification.constant;

/**
 * FCM Silent 메시지(data-only)의 타입을 정의하는 열거형.
 *
 * <p>Silent 메시지는 알림 표시 없이 앱에 특정 동작을 지시하기 위해 사용된다.
 * iOS에서는 {@code content-available: 1}, Android에서는 HIGH priority로 전송된다.
 */
public enum FcmSilentType {

    /** 특정 기기를 강제 로그아웃 처리하도록 앱에 지시한다. */
    FORCE_LOGOUT,

    /** 이클래스 토큰이 무효해졌으니 재발급 후 다시 연동하도록 앱에 지시한다. */
    ECLASS_RELINK
}
