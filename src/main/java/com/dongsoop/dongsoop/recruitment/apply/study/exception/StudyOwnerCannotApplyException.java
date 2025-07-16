package com.dongsoop.dongsoop.recruitment.apply.study.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class StudyOwnerCannotApplyException extends CustomException {

    public StudyOwnerCannotApplyException(Long boardId) {
        super("스터디 모집 게시글의 주인은 자신의 모집글에 지원할 수 없습니다. 게시글Id: " + boardId, HttpStatus.BAD_REQUEST);
    }
}
