package com.dongsoop.dongsoop.exception.domain.jwt;

import com.dongsoop.dongsoop.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TokenExpiredException extends CustomException {

        public TokenExpiredException() {
            super("토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED);
        }

}
