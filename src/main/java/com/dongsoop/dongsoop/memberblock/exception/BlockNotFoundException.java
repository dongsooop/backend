package com.dongsoop.dongsoop.memberblock.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class BlockNotFoundException extends CustomException {

    public BlockNotFoundException() {
        super("해당 회원을 차단한 기록이 존재하지 않습니다.", HttpStatus.NOT_FOUND);
    }
}
