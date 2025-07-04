package com.dongsoop.dongsoop.recruitment.study.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class StudyBoardDepartmentNotAssignedException extends CustomException {

    public StudyBoardDepartmentNotAssignedException(Long boardId) {
        super("스터디 모집 게시글에 학과가 지정되지 않았습니다. 게시글 Id: " + boardId, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
