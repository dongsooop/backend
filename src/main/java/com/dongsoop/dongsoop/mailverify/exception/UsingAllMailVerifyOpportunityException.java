package com.dongsoop.dongsoop.mailverify.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UsingAllMailVerifyOpportunityException extends CustomException {

    public UsingAllMailVerifyOpportunityException() {
        super("모든 메일 인증 기회를 사용하였습니다. 인증 코드를 새로 발급해주세요..", HttpStatus.BAD_REQUEST);
    }
}
