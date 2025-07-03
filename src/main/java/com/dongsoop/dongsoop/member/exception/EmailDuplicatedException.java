package com.dongsoop.dongsoop.member.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class EmailDuplicatedException extends CustomException {

    public EmailDuplicatedException() {
        super("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT);
    }

}
