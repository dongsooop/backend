package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UnsupportedSanctionTypeException extends CustomException {

    public UnsupportedSanctionTypeException() {
        super("메시지 신고에는 게시글 삭제 제재를 쓸 수 없습니다.", HttpStatus.BAD_REQUEST);
    }
}
