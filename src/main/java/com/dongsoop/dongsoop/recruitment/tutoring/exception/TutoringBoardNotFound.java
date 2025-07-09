package com.dongsoop.dongsoop.recruitment.tutoring.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TutoringBoardNotFound extends CustomException {

    public TutoringBoardNotFound(Long id) {
        super("해당 튜터링 모집글이 존재하지 않습니다: " + id, HttpStatus.NOT_FOUND);
    }

    public TutoringBoardNotFound(Long studyBoardId, Long memberId) {
        super("해당 튜터링 모집글이 존재하지 않습니다: 게시글 ID: " + studyBoardId + ", 회원 ID: " + memberId, HttpStatus.NOT_FOUND);
    }
}
