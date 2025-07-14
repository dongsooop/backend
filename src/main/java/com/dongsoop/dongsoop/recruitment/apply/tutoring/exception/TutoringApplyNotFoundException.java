package com.dongsoop.dongsoop.recruitment.apply.tutoring.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TutoringApplyNotFoundException extends CustomException {

    public TutoringApplyNotFoundException(Long boardId, Long applierId) {
        super(boardId + " 게시글에 대해 " + applierId + " 지원자가 지원한 기록이 없습니다.", HttpStatus.NOT_FOUND);
    }
}
