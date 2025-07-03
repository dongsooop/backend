package com.dongsoop.dongsoop.member.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MemberNotFoundException extends CustomException {

    public MemberNotFoundException() {
        super("회원 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    }

}
