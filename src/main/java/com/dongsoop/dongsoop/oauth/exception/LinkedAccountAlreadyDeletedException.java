package com.dongsoop.dongsoop.oauth.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class LinkedAccountAlreadyDeletedException extends CustomException {

    public LinkedAccountAlreadyDeletedException() {
        super("소셜 계정과 연결된 회원이 탈퇴상태입니다.", HttpStatus.CONFLICT);
    }
}
