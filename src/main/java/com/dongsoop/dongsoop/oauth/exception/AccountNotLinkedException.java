package com.dongsoop.dongsoop.oauth.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class AccountNotLinkedException extends CustomException {

    public AccountNotLinkedException() {
        super("소셜 계정이 연결되어 있지 않습니다.", HttpStatus.BAD_REQUEST);
    }
}
