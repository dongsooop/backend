package com.dongsoop.dongsoop.handler;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class JwtLogoutHandler implements LogoutHandler {

    @Value("${jwt.refreshToken.cookie.name}")
    private String refreshTokenCookieName;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Cookie cookie = new Cookie(refreshTokenCookieName, null);
        cookie.setMaxAge(0);        // 쿠키 즉시 삭제
        cookie.setPath("/");        // 전체 경로에 대한 쿠키
        cookie.setHttpOnly(true);   // 클라이언트 접근 불간

        response.addCookie(cookie);
    }

}
