package com.dongsoop.dongsoop.member.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NicknameDuplicatedException extends CustomException {

    public NicknameDuplicatedException() {
        super("이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT);
    }

}
