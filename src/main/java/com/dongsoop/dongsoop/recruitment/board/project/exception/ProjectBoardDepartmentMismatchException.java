package com.dongsoop.dongsoop.recruitment.board.project.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import java.util.List;
import org.springframework.http.HttpStatus;

public class ProjectBoardDepartmentMismatchException extends CustomException {

    public ProjectBoardDepartmentMismatchException(List<DepartmentType> boardDepartmentList,
                                                   DepartmentType requesterDepartment) {
        super("지원자의 학과가 프로젝트 모집 게시글의 학과와 일치하지 않습니다. 게시글 모집 학과: " + boardDepartmentList + ", 지원자 학과: "
                        + requesterDepartment.name(),
                HttpStatus.BAD_REQUEST);
    }
}
