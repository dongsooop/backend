package com.dongsoop.dongsoop.exception.domain.websocket;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UserNotInRoomException extends CustomException {
    public UserNotInRoomException() {
        super("사용자가 이 채팅방에 존재하지 않습니다", HttpStatus.BAD_REQUEST);
    }
}