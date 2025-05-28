package com.dongsoop.dongsoop.jwt.controller;

import com.dongsoop.dongsoop.exception.domain.jwt.RefreshTokenNotFoundException;
import com.dongsoop.dongsoop.jwt.dto.IssuedToken;
import com.dongsoop.dongsoop.jwt.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/token")
@RequiredArgsConstructor
public class JwtController {

    private final JwtService jwtService;

    @Value("${jwt.refreshToken.cookie.name}")
    private String refreshTokenCookieName;

    @Value("${jwt.expired-time.refresh-token}")
    private Long refreshTokenExpiredTime;

    @GetMapping("/reissue")
    public ResponseEntity<String> reissueByRefreshToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        Cookie refreshCookie = getRefreshTokenCookie(cookies);

        String refreshToken = refreshCookie.getValue();
        IssuedToken issuedToken = jwtService.issuedTokenByRefreshToken(refreshToken);
        Cookie newRefreshCookie = issuedNewRefreshTokenCookie(issuedToken.getRefreshToken());
        response.addCookie(newRefreshCookie);

        return ResponseEntity.ok(issuedToken.getAccessToken());
    }

    private Cookie issuedNewRefreshTokenCookie(String newRefreshToken) {
        Cookie newRefreshCookie = new Cookie(refreshTokenCookieName, newRefreshToken);
        newRefreshCookie.setHttpOnly(true);
        newRefreshCookie.setSecure(true);
        newRefreshCookie.setPath("/");
        newRefreshCookie.setMaxAge((int) (refreshTokenExpiredTime / 1000));

        return newRefreshCookie;
    }

    private Cookie getRefreshTokenCookie(Cookie[] cookies) {
        if (cookies == null) {
            throw new RefreshTokenNotFoundException();
        }

        for (Cookie cookie : cookies) {
            if (refreshTokenCookieName.equals(cookie.getName())) {
                return cookie;
            }
        }

        throw new RefreshTokenNotFoundException();
    }
}
