package com.dongsoop.dongsoop.exception.domain.member;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class EmailDuplicatedException extends CustomException {

    public EmailDuplicatedException() {
        super("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT);
    }

}