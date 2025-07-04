package com.dongsoop.dongsoop.chat.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class SelfChatException extends CustomException {
    public SelfChatException() {
        super("자기 자신과 채팅을 시작할 수 없습니다. ", HttpStatus.BAD_REQUEST);
    }
}
