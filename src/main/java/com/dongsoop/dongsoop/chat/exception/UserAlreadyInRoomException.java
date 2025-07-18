package com.dongsoop.dongsoop.chat.exception;
import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UserAlreadyInRoomException extends CustomException {
    public UserAlreadyInRoomException(Long userId) {
        super("사용자 " + userId + "는 이미 채팅방에 참여 중입니다.", HttpStatus.BAD_REQUEST);
    }
}