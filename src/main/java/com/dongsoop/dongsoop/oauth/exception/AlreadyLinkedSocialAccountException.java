package com.dongsoop.dongsoop.oauth.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class AlreadyLinkedSocialAccountException extends CustomException {

    public AlreadyLinkedSocialAccountException() {
        super("이미 연동된 소셜 계정입니다.", HttpStatus.CONFLICT);
    }
}
