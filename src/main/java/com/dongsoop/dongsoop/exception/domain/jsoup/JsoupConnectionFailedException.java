package com.dongsoop.dongsoop.exception.domain.jsoup;

public class JsoupConnectionFailedException extends RuntimeException {

    public JsoupConnectionFailedException() {
        super("Jsoup 커넥션 연결에 실패하였습니다");
    }

}
