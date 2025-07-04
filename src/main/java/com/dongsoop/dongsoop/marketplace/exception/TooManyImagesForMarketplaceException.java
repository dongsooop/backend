package com.dongsoop.dongsoop.marketplace.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TooManyImagesForMarketplaceException extends CustomException {

    public TooManyImagesForMarketplaceException(int amount) {
        super("장터 게시글에 첨부할 수 있는 이미지의 개수는 최대 " + amount + "개입니다.", HttpStatus.BAD_REQUEST);
    }
}
