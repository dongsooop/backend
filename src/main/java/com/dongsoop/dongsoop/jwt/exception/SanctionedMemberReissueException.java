package com.dongsoop.dongsoop.jwt.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import com.dongsoop.dongsoop.report.exception.MemberSanctionedException;
import org.springframework.http.HttpStatus;

public class SanctionedMemberReissueException extends CustomException {

    public SanctionedMemberReissueException(MemberSanctionedException cause) {
        super(cause.getMessage(), HttpStatus.UNAUTHORIZED, cause);
    }
}
