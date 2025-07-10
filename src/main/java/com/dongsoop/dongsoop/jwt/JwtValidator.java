package com.dongsoop.dongsoop.jwt;

import com.dongsoop.dongsoop.jwt.exception.NotAccessTokenException;
import com.dongsoop.dongsoop.jwt.exception.NotRefreshTokenException;
import com.dongsoop.dongsoop.jwt.exception.TokenMalformedException;
import com.dongsoop.dongsoop.jwt.exception.TokenRoleNotAvailableException;
import io.jsonwebtoken.Claims;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtValidator {

    @Value("${jwt.claims.role.name}")
    private String roleClaimName;

    @Value("${jwt.claims.type.name}")
    private String typeClaimName;

    public void validate(Claims claims) {
        // 요청자 ID 검사
        if (!claims.getSubject().matches("\\d+")) {
            throw new TokenMalformedException();
        }

        // 토큰 내용 검사
        validateClaims(claims);
    }

    private void validateClaims(Claims claims) {
        List<?> auth = claims.get(roleClaimName, List.class);

        if (auth == null || auth.isEmpty()) {
            throw new TokenRoleNotAvailableException();
        }

        // 타입 체크 시 JWTType enum에 존재하지 않는 경우 IllegalArgumentException 발생
        validateTokenType(claims);
    }

    private JWTType validateTokenType(Claims claims) {
        try {
            String type = claims.get(typeClaimName, String.class);
            if (!StringUtils.hasText(type)) {
                throw new TokenMalformedException();
            }

            return JWTType.valueOf(type);
        } catch (IllegalArgumentException exception) {
            throw new TokenMalformedException();
        }
    }

    public void validateAccessToken(Claims claims) {
        JWTType type = validateTokenType(claims);

        if (!JWTType.ACCESS.equals(type)) {
            throw new NotAccessTokenException();
        }
    }

    public void validateRefreshToken(Claims claims) {
        JWTType type = validateTokenType(claims);

        if (!JWTType.REFRESH.equals(type)) {
            throw new NotRefreshTokenException();
        }
    }
}
