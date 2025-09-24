package com.dongsoop.dongsoop.appcheck.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UnknownFirebaseFetchJWKException extends CustomException {

    public UnknownFirebaseFetchJWKException() {
        super("JWK를 가져오는데 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public UnknownFirebaseFetchJWKException(Throwable e) {
        super("JWK를 가져오는데 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, e);
    }
}
