package com.dongsoop.dongsoop.notice.reminder.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NoticeReminderNotFoundException extends CustomException {

    public NoticeReminderNotFoundException() {
        super("공지 리마인더를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    }
}
