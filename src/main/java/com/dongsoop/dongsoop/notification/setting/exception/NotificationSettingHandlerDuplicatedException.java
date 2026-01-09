package com.dongsoop.dongsoop.notification.setting.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import com.dongsoop.dongsoop.notification.setting.service.handler.NotificationSettingChangeHandler;
import org.springframework.http.HttpStatus;

public class NotificationSettingHandlerDuplicatedException extends CustomException {

    public NotificationSettingHandlerDuplicatedException(
            NotificationSettingChangeHandler existing,
            NotificationSettingChangeHandler duplicated) {

        super("알림 설정 처리기가 중복으로 등록되어 있습니다: " + existing.getClass().getName() + ", "
                + duplicated.getClass().getName(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
