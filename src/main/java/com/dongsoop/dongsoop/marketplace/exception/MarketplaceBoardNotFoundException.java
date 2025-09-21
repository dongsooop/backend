package com.dongsoop.dongsoop.marketplace.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MarketplaceBoardNotFoundException extends CustomException {

    public MarketplaceBoardNotFoundException(Long boardId) {
        super("존재하지 않는 장터 게시글입니다. 게시글 ID: " + boardId, HttpStatus.NOT_FOUND);
    }
}
