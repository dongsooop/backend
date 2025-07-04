package com.dongsoop.dongsoop.department.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import org.springframework.http.HttpStatus;

public class DepartmentNotFoundException extends CustomException {

    public DepartmentNotFoundException(DepartmentType departmentType) {
        super("해당 학과가 존재하지 않습니다: " + departmentType.name(), HttpStatus.NOT_FOUND);
    }
}
