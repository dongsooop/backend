package com.dongsoop.dongsoop.marketplace.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MarketplaceBoardAlreadyClosedException extends CustomException {

    public MarketplaceBoardAlreadyClosedException(Long boardId) {
        super("이미 종료된 장터 글입니다. 게시글 ID: " + boardId, HttpStatus.BAD_REQUEST);
    }
}
