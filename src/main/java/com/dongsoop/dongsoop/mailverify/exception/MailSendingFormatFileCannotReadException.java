package com.dongsoop.dongsoop.mailverify.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import java.io.IOException;
import org.springframework.http.HttpStatus;

public class MailSendingFormatFileCannotReadException extends CustomException {

    public MailSendingFormatFileCannotReadException() {
        super(
                "인증 메일 포맷 파일의 경로를 읽을 수 없습니다.",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    public MailSendingFormatFileCannotReadException(IOException exception) {
        super(
                "인증 메일 포맷 파일의 경로를 읽을 수 없습니다: " + exception.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception
        );
    }
}
