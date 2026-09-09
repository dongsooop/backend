package com.dongsoop.dongsoop.notice.reminder.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NoticeDetailsNotFoundException extends CustomException {

    public NoticeDetailsNotFoundException(Long noticeId) {
        super("공지를 찾을 수 없습니다: " + noticeId, HttpStatus.NOT_FOUND);
    }
}
