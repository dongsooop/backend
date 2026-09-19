package com.dongsoop.dongsoop.s3.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidFileNameException extends CustomException {

    public InvalidFileNameException(String fileName) {
        super("확장자를 가진 파일 이름이 필요합니다: " + fileName, HttpStatus.BAD_REQUEST);
    }
}
