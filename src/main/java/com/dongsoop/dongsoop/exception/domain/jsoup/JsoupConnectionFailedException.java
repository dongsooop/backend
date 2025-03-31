package com.dongsoop.dongsoop.exception.domain.jsoup;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class JsoupConnectionFailedException extends CustomException {

    public JsoupConnectionFailedException() {
        super("Jsoup 커넥션 연결에 실패하였습니다", HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
