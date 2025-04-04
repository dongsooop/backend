package com.dongsoop.dongsoop.exception.domain.parser;

public class NoticeLinkNotAvailableException extends RuntimeException {

    public NoticeLinkNotAvailableException() {
        super("공지사항 링크가 유효하지 않습니다");
    }
}
