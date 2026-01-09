package com.dongsoop.dongsoop.notification.setting.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import com.dongsoop.dongsoop.notification.setting.dto.SettingChanges;
import org.springframework.http.HttpStatus;

public class NotificationSettingHandlerNotFoundException extends CustomException {

    public NotificationSettingHandlerNotFoundException(Class<? extends SettingChanges> clazz) {
        super("SettingChanges 유형에 대해 등록된 handler가 없습니다: " + clazz.getName(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
