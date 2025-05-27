package com.dongsoop.dongsoop.exception.domain.websocket;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidChatRequestException extends CustomException {
    public InvalidChatRequestException() {
        super("잘못된 채팅요청입니다.", HttpStatus.BAD_REQUEST);
    }
}
