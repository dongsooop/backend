package com.dongsoop.dongsoop.recruitment.study.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class StudyBoardNotFound extends CustomException {

    public StudyBoardNotFound(Long studyBoardId) {
        super("해당 스터디 모집글이 존재하지 않습니다: " + studyBoardId, HttpStatus.NOT_FOUND);
    }
}
