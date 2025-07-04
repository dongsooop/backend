package com.dongsoop.dongsoop.recruitment.project.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ProjectBoardNotFound extends CustomException {

    public ProjectBoardNotFound(Long projectBoardId) {
        super("해당 프로젝트 모집글이 존재하지 않습니다: " + projectBoardId, HttpStatus.NOT_FOUND);
    }
}
