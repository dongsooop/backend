package com.dongsoop.dongsoop.jwt;

import com.dongsoop.dongsoop.jwt.exception.NotAccessTokenException;
import com.dongsoop.dongsoop.jwt.exception.NotRefreshTokenException;
import com.dongsoop.dongsoop.jwt.exception.TokenExpiredException;
import com.dongsoop.dongsoop.jwt.exception.TokenMalformedException;
import com.dongsoop.dongsoop.jwt.exception.TokenRoleNotAvailableException;
import com.dongsoop.dongsoop.jwt.exception.TokenUnsupportedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtValidator {

    private final JwtUtil jwtUtil;

    @Value("${jwt.claims.role.name}")
    private String roleClaimName;

    @Value("${jwt.claims.type.name}")
    private String typeClaimName;

    public Claims validate(String token) {
        if (!StringUtils.hasText(token)) {
            throw new TokenMalformedException();
        }

        try {
            Claims claims = jwtUtil.getClaims(token);
            Long.valueOf(claims.getSubject());
            validateClaims(claims);

            return claims;
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException(e);
        } catch (MalformedJwtException e) {
            throw new TokenMalformedException(e);
        } catch (Exception e) {
            throw new TokenUnsupportedException(e);
        }
    }

    private void validateClaims(Claims claims) {
        List<?> auth = claims.get(roleClaimName, List.class);
        String type = claims.get(typeClaimName, String.class);

        if (auth == null || auth.isEmpty()) {
            throw new TokenRoleNotAvailableException();
        }
        if (type == null) {
            throw new TokenMalformedException();
        }

        // 타입 체크 시 JWTType enum에 존재하지 않는 경우 IllegalArgumentException 발생
        try {
            JWTType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new TokenMalformedException();
        }
    }

    public void validateAccessToken(String token) {
        Claims claims = validate(token);
        String type = claims.get(typeClaimName, String.class);

        if (!JWTType.ACCESS.name().equals(type)) {
            throw new NotAccessTokenException();
        }
    }

    public void validateRefreshToken(String token) {
        Claims claims = validate(token);
        String type = claims.get(typeClaimName, String.class);

        if (!JWTType.REFRESH.name().equals(type)) {
            throw new NotRefreshTokenException();
        }
    }
}
