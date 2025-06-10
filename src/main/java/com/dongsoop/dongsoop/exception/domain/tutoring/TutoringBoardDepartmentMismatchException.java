package com.dongsoop.dongsoop.exception.domain.tutoring;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TutoringBoardDepartmentMismatchException extends CustomException {

    public TutoringBoardDepartmentMismatchException(DepartmentType boardDepartment,
                                                    DepartmentType requesterDepartment) {
        super("튜터링 모집 학과가 지원자의 학과와 일치하지 않습니다. 게시글 모집 학과: " + boardDepartment.name() + ", 지원자 학과: "
                        + requesterDepartment.name(),
                HttpStatus.BAD_REQUEST);
    }
}
