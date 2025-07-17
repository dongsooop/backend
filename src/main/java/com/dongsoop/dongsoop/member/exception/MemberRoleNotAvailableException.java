package com.dongsoop.dongsoop.member.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MemberRoleNotAvailableException extends CustomException {

    public MemberRoleNotAvailableException(String roles) {
        super("회원의 역할이 유효하지 않습니다: " + roles, HttpStatus.FORBIDDEN);
    }
}
