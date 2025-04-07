package com.dongsoop.dongsoop.exception.domain.department;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class DepartmentNotFoundException extends CustomException {

    public DepartmentNotFoundException() {
        super("해당 학과가 존재하지 않습니다.", HttpStatus.NOT_FOUND);
    }
}
