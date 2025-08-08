package com.dongsoop.dongsoop.notification.exception;

public class NotificationSendException extends RuntimeException {

    public NotificationSendException(Exception exception) {
        super("알림 전송에 실패했습니다. 잠시 후 다시 시도해주세요.", exception);
    }
}
