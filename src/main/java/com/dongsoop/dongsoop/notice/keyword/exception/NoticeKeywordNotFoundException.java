package com.dongsoop.dongsoop.notice.keyword.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NoticeKeywordNotFoundException extends CustomException {

    public NoticeKeywordNotFoundException(Long keywordId) {
        super("공지 키워드를 찾을 수 없습니다. id: " + keywordId, HttpStatus.NOT_FOUND);
    }
}