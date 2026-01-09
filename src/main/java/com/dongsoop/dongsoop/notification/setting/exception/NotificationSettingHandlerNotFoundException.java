package com.dongsoop.dongsoop.notification.setting.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NotificationSettingHandlerNotFoundException extends CustomException {

    public NotificationSettingHandlerNotFoundException() {
        super("알림 설정 핸들러를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
