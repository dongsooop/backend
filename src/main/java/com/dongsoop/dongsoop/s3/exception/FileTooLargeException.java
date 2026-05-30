package com.dongsoop.dongsoop.s3.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class FileTooLargeException extends CustomException {

    public FileTooLargeException(long maxSizeBytes) {
        super("업로드 가능한 최대 파일 크기(" + (maxSizeBytes / (1024 * 1024)) + "MB)를 초과했습니다.",
                HttpStatus.PAYLOAD_TOO_LARGE);
    }
}
