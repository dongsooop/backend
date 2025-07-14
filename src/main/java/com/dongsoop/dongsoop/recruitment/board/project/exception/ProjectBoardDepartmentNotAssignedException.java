package com.dongsoop.dongsoop.recruitment.board.project.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ProjectBoardDepartmentNotAssignedException extends CustomException {

    public ProjectBoardDepartmentNotAssignedException(Long boardId) {
        super("프로젝트 모집 게시글에 학과가 지정되지 않았습니다. 게시글 Id: " + boardId, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
