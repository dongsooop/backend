package com.dongsoop.dongsoop.recruitment.apply.tutoring.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TutoringOwnerCannotApplyException extends CustomException {

    public TutoringOwnerCannotApplyException(Long boardId) {
        super("튜터링 모집 게시글의 주인은 자신의 모집글에 지원할 수 없습니다. 게시글Id: " + boardId, HttpStatus.BAD_REQUEST);
    }
}
