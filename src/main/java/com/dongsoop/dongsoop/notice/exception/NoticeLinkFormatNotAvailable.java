package com.dongsoop.dongsoop.notice.exception;

public class NoticeLinkFormatNotAvailable extends RuntimeException {

    public NoticeLinkFormatNotAvailable(String link) {
        super("공지사항 링크 형식이 유효하지 않습니다: " + link);
    }

}
