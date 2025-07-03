package com.dongsoop.dongsoop.recruitment.tutoring.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TutoringBoardNotFound extends CustomException {

    public TutoringBoardNotFound(Long id) {
        super("존재하지 않는 튜터링 게시글입니다: " + id, HttpStatus.NOT_FOUND);
    }
}
