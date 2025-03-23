package com.dongsoop.dongsoop.exception.domain.member;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MemberNotFoundException extends CustomException {

    public MemberNotFoundException() {
        super("회원 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    }

}