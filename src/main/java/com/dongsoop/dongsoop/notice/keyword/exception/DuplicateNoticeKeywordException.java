package com.dongsoop.dongsoop.notice.keyword.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class DuplicateNoticeKeywordException extends CustomException {

    public DuplicateNoticeKeywordException(String keyword) {
        super("이미 등록된 공지 키워드입니다: " + keyword, HttpStatus.CONFLICT);
    }
}
