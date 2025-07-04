package com.dongsoop.dongsoop.marketplace.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MarketplaceAlreadyContactException extends CustomException {

    public MarketplaceAlreadyContactException(Long memberId, Long boardId) {
        super("이미 해당 마켓플레이스에 지원한 상태입니다. 사용자 ID: " + memberId + ", 게시글 Id: " + boardId, HttpStatus.BAD_REQUEST);
    }
}
