package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class NotBlindDateParticipantException extends CustomException {

    public NotBlindDateParticipantException() {
        super("과팅에 참여 중인 회원만 신고할 수 있습니다.", HttpStatus.FORBIDDEN);
    }
}
