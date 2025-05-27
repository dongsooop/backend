package com.dongsoop.dongsoop.exception.domain.notice;

public class NoticeSubjectNotAvailableException extends RuntimeException {

    public NoticeSubjectNotAvailableException() {
        super("공지사항 제목 파싱 중 오류 발생");
    }
}
