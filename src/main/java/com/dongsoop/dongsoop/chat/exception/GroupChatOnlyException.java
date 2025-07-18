package com.dongsoop.dongsoop.chat.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class GroupChatOnlyException extends CustomException {
    public GroupChatOnlyException(String action) {
        super(action + "은(는) 그룹 채팅방에서만 가능합니다.", HttpStatus.BAD_REQUEST);
    }
}