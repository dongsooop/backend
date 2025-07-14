package com.dongsoop.dongsoop.recruitment.apply.project.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ProjectApplyNotFoundException extends CustomException {

    public ProjectApplyNotFoundException(Long boardId, Long applierId) {
        super(boardId + " 게시글에 대해 " + applierId + " 지원자가 지원한 기록이 없습니다.", HttpStatus.NOT_FOUND);
    }
}
