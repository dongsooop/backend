package com.dongsoop.dongsoop.notification.setting.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NotificationTypeNotFoundException extends CustomException {

    public NotificationTypeNotFoundException() {
        super("변경하려는 알림 유형을 찾을 수 없습니다.", HttpStatus.BAD_GATEWAY);
    }
}
