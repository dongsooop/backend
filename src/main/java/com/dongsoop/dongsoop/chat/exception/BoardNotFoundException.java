package com.dongsoop.dongsoop.chat.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class BoardNotFoundException extends CustomException {
    public BoardNotFoundException() {
        super("해당 게시판의 게시글이 존재하지 않습니다.", HttpStatus.NOT_FOUND);
    }
}
