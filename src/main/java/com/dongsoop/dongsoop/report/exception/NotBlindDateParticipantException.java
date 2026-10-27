package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NotBlindDateParticipantException extends CustomException {

    public NotBlindDateParticipantException() {
        super("해당 과팅 세션의 참가자만 신고할 수 있습니다.", HttpStatus.FORBIDDEN);
    }
}
