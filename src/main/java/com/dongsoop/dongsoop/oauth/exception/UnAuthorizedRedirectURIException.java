package com.dongsoop.dongsoop.oauth.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UnAuthorizedRedirectURIException extends CustomException {

    public UnAuthorizedRedirectURIException() {
        super("허용되지 않은 리다이렉트 URI 입니다.", HttpStatus.UNAUTHORIZED);
    }
}
