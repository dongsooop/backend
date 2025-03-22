package com.dongsoop.dongsoop.jwt;

import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class TokenGenerator {

    @Value("${jwt.expired-time.access-token}")
    private Long accessTokenExpiredTime;

    @Value("${jwt.expired-time.refresh-token}")
    private Long refreshTokenExpiredTime;

    private final JwtUtil jwtUtil;

    public String generateAccessToken(Authentication authentication) {
        long now = (new Date()).getTime();
        Date expireAt = new Date(now + this.accessTokenExpiredTime);

        return jwtUtil.issue(expireAt, authentication);
    }

    public String generateRefreshToken(Authentication authentication) {
        long now = (new Date()).getTime();
        Date expireAt = new Date(now + this.refreshTokenExpiredTime);

        return jwtUtil.issue(expireAt, authentication);
    }

}
