package com.dongsoop.dongsoop.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.claims.name}")
    private String claimName;

    private final JwtKeyManager jwtKeyManager;

    protected Claims getClaims(String token) {
        SecretKey key = jwtKeyManager.getSecretKey();

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    protected String issue(Date tokenExpiredTime, Authentication authentication) {
        SecretKey key = jwtKeyManager.getSecretKey();

        return Jwts.builder()
                .subject(authentication.getName())
                .claim(claimName, authentication.getAuthorities())
                .signWith(key)
                .expiration(tokenExpiredTime)
                .compact();
    }

    public String getNameByToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

}
