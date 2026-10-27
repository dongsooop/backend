package com.dongsoop.dongsoop.report.exception;

import com.dongsoop.dongsoop.common.exception.CustomException;
import com.dongsoop.dongsoop.report.dto.SanctionStatusResponse;
import com.dongsoop.dongsoop.report.entity.Sanction;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class MemberSanctionedException extends CustomException {

    private static final String MESSAGE = "회원님은 현재 제재 중입니다. 자세한 내용은 고객센터에 문의해주세요.";

    private final SanctionStatusResponse sanction;

    public MemberSanctionedException(Sanction sanction) {
        super(MESSAGE, HttpStatus.FORBIDDEN);
        this.sanction = SanctionStatusResponse.withSanction(sanction);
    }
}
