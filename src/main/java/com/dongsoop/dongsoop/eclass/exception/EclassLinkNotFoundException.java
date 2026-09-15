package com.dongsoop.dongsoop.eclass.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class EclassLinkNotFoundException extends CustomException {

    public EclassLinkNotFoundException() {
        super("이클래스 연동 정보가 없습니다.", HttpStatus.NOT_FOUND);
    }
}
