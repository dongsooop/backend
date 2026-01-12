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

    private final JwtUtil jwtUtil;

    @Value("${jwt.expired-time.social-token}")
    private Long socialTokenExpiredTime;

    @Value("${jwt.expired-time.access-token}")
    private Long accessTokenExpiredTime;

    @Value("${jwt.expired-time.refresh-token}")
    private Long refreshTokenExpiredTime;

    public String generateAccessToken(Authentication authentication) {
        long now = (new Date()).getTime();
        Date expireAt = new Date(now + this.accessTokenExpiredTime);

        String id = authentication.getName();
        List<String> roleList = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return jwtUtil.issue(expireAt, id, roleList, JWTType.ACCESS);
    }

    public String generateRefreshToken(Authentication authentication) {
        long now = (new Date()).getTime();
        Date expireAt = new Date(now + this.refreshTokenExpiredTime);

        String id = authentication.getName();
        List<String> roleList = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return jwtUtil.issue(expireAt, id, roleList, JWTType.REFRESH);
    }

    public String generateSocialToken(Authentication authentication) {
        long now = (new Date()).getTime();
        Date expireAt = new Date(now + this.socialTokenExpiredTime);

        String id = authentication.getName();
        List<String> roleList = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return jwtUtil.issue(expireAt, id, roleList, JWTType.REFRESH);
    }
}
