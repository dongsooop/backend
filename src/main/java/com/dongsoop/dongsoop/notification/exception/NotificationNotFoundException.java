package com.dongsoop.dongsoop.notification.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NotificationNotFoundException extends CustomException {

    public NotificationNotFoundException() {
        super("존재하지 않는 알림입니다.", HttpStatus.NOT_FOUND);
    }
}
