package com.dongsoop.dongsoop.recruitment.study.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class StudyRecruitmentAlreadyAppliedException extends CustomException {

    public StudyRecruitmentAlreadyAppliedException(Long memberId, Long boardId) {
        super("이미 지원한 스터디 모집 게시글입니다. 사용자 ID: " + memberId + ", 게시글 ID: " + boardId, HttpStatus.BAD_REQUEST);
    }
}
