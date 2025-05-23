package com.dongsoop.dongsoop.exception.domain.websocket;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UserKickedException extends CustomException {
    public UserKickedException(String roomId) {
        super("이 채팅방에서 강퇴되어 접근할 수 없습니다: " + roomId, HttpStatus.FORBIDDEN);
    }
}