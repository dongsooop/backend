package com.dongsoop.dongsoop.notice.reminder.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidNoticeReminderTimeException extends CustomException {

    public InvalidNoticeReminderTimeException() {
        super("리마인더 시간은 현재 시각 이후여야 합니다.", HttpStatus.BAD_REQUEST);
    }
}
