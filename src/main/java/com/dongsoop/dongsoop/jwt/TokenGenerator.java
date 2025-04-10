package com.dongsoop.dongsoop.jwt;

import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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

        String id = authentication.getName();
        List<String> roleList = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return jwtUtil.issue(expireAt, id, roleList);
    }

    public String generateRefreshToken(Authentication authentication) {
        long now = (new Date()).getTime();
        Date expireAt = new Date(now + this.refreshTokenExpiredTime);

        String id = authentication.getName();
        List<String> roleList = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return jwtUtil.issue(expireAt, id, roleList);
    }

}
