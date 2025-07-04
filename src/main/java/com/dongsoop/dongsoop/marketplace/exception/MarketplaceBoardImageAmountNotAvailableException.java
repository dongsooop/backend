package com.dongsoop.dongsoop.marketplace.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class MarketplaceBoardImageAmountNotAvailableException extends CustomException {

    public MarketplaceBoardImageAmountNotAvailableException(int imageAmount) {
        super("장터 게시글에 이미지 수가 유효하지 않은 범위입니다. 이미지 수: " + imageAmount, HttpStatus.BAD_REQUEST);
    }
}
