package com.dongsoop.dongsoop.chat.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ManagerKickAttemptException extends CustomException {
    public ManagerKickAttemptException() {
        super("매니저는 추방할 수 없습니다", HttpStatus.BAD_REQUEST);
    }
}
