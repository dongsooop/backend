package com.dongsoop.dongsoop.recruitment.apply.project.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ProjectRecruitmentAlreadyAppliedException extends CustomException {

    public ProjectRecruitmentAlreadyAppliedException(Long memberId, Long boardId) {
        super("이미 지원한 프로젝트 모집 게시글입니다. 사용자 ID: " + memberId + ", 게시글 ID: " + boardId, HttpStatus.BAD_REQUEST);
    }
}
