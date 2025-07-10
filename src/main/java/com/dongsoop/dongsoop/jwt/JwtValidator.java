package com.dongsoop.dongsoop.jwt;

import com.dongsoop.dongsoop.jwt.exception.NotAccessTokenException;
import com.dongsoop.dongsoop.jwt.exception.NotRefreshTokenException;
import com.dongsoop.dongsoop.jwt.exception.TokenExpiredException;
import com.dongsoop.dongsoop.jwt.exception.TokenMalformedException;
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

    public void validate(String token) {
        if (!StringUtils.hasText(token)) {
            throw new TokenMalformedException();
        }

        try {
            Claims claims = jwtUtil.getClaims(token);
            Long.valueOf(claims.getSubject());
            claims.get(roleClaimName, List.class);
            claims.get(typeClaimName, String.class);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException(e);
        } catch (MalformedJwtException e) {
            throw new TokenMalformedException(e);
        } catch (Exception e) {
            throw new TokenUnsupportedException(e);
        }
    }

    public void validateAccessToken(String token) {
        validate(token);
        Claims claims = jwtUtil.getClaims(token);
        String type = claims.get(typeClaimName, String.class);

        if (!type.equals(JWTType.ACCESS.name())) {
            throw new NotAccessTokenException();
        }
    }

    public void validateRefreshToken(String token) {
        validate(token);
        Claims claims = jwtUtil.getClaims(token);
        String type = claims.get(typeClaimName, String.class);

        if (!type.equals(JWTType.REFRESH.name())) {
            throw new NotRefreshTokenException();
        }
    }
}
