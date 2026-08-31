package com.dongsoop.dongsoop.eclass.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class EclassSyncCooldownException extends CustomException {

    public EclassSyncCooldownException() {
        super("잠시 후 다시 시도해 주세요.", HttpStatus.TOO_MANY_REQUESTS);
    }
}
