package com.dongsoop.dongsoop.exception.domain.member;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NicknameDuplicatedException extends CustomException {

    public NicknameDuplicatedException() {
        super("이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT);
    }

}
