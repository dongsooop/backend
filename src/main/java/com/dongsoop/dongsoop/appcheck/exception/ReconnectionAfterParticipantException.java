package com.dongsoop.dongsoop.appcheck.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ReconnectionAfterParticipantException extends CustomException {

    public ReconnectionAfterParticipantException() {
        super("참여 후 재접속한 사용자입니다.", HttpStatus.CONFLICT);
    }
}
