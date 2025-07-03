package com.dongsoop.dongsoop.chat.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UnauthorizedChatAccessException extends CustomException {

    public UnauthorizedChatAccessException() {
        super("채팅방에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }

    public UnauthorizedChatAccessException(Throwable cause) {
        super("채팅방에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN, cause);
    }
}
