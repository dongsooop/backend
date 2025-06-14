package com.dongsoop.dongsoop.exception.domain.s3;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class S3UnknownException extends CustomException {

    public S3UnknownException(Exception exception) {
        super("S3 사용중 문제가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }
}
