package com.dongsoop.dongsoop.exception.custom;

import com.dongsoop.dongsoop.exception.CustomException;
import com.dongsoop.dongsoop.exception.ErrorCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class MemberException extends CustomException {
    private HttpStatus httpStatus;

    public MemberException(final ErrorCode errorCode) {
        super(errorCode);
        this.httpStatus = errorCode.getStatus();
    }
}
