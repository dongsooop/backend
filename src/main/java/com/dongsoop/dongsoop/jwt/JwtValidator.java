package com.dongsoop.dongsoop.jwt;

import com.dongsoop.dongsoop.exception.domain.jwt.TokenExpiredException;
import com.dongsoop.dongsoop.exception.domain.jwt.TokenMalformedException;
import com.dongsoop.dongsoop.exception.domain.jwt.TokenUnsupportedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtValidator {

    private final JwtUtil jwtUtil;

    public void validate(String token) {
        try {
            Claims claims = jwtUtil.getClaims(token);
            claims.getSubject();
            claims.getExpiration();

            if (token == null || token.isBlank() || isExpired(token)) {
                throw new TokenMalformedException();
            }
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException(e);
        } catch (MalformedJwtException e) {
            throw new TokenMalformedException(e);
        } catch (Exception e) {
            throw new TokenUnsupportedException(e);
        }
    }

    public boolean isExpired(String token) {
        try {
            Claims claims = jwtUtil.getClaims(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            throw new TokenExpiredException();
        }
    }

}
