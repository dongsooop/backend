package com.dongsoop.dongsoop.chat.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ChatRoomNotFoundException extends CustomException {
    public ChatRoomNotFoundException() {
        super("채팅방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    }
}
