package com.dongsoop.dongsoop.exception.domain.tutoring;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TutoringRecruitmentAlreadyAppliedException extends CustomException {

    public TutoringRecruitmentAlreadyAppliedException(Long memberId, Long boardId) {
        super("이미 지원한 튜터링 모집 게시글입니다. 사용자 ID: " + memberId + ", 게시글 ID: " + boardId, HttpStatus.BAD_REQUEST);
    }
}
