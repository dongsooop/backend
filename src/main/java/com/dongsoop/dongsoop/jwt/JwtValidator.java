package com.dongsoop.dongsoop.jwt;

import com.dongsoop.dongsoop.exception.domain.jwt.TokenExpiredException;
import com.dongsoop.dongsoop.exception.domain.jwt.TokenMalformedException;
import com.dongsoop.dongsoop.exception.domain.jwt.TokenUnsupportedException;
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

    public void validate(String token) {
        if (StringUtils.hasText(token)) {
            throw new TokenMalformedException();
        }

        try {
            Claims claims = jwtUtil.getClaims(token);
            Long.valueOf(claims.getSubject());
            claims.get(roleClaimName, List.class);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException(e);
        } catch (MalformedJwtException e) {
            throw new TokenMalformedException(e);
        } catch (Exception e) {
            throw new TokenUnsupportedException(e);
        }
    }
}
