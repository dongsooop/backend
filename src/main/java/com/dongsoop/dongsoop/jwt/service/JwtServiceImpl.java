package com.dongsoop.dongsoop.jwt.service;

import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.jwt.dto.IssuedToken;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtValidator jwtValidator;

    private final TokenGenerator tokenGenerator;

    private final JwtUtil jwtUtil;

    public IssuedToken issuedTokenByRefreshToken(String refreshToken) {
        Claims claims = jwtUtil.getClaims(refreshToken);
        jwtValidator.validate(claims);
        jwtValidator.validateRefreshToken(claims);

        Authentication authentication = jwtUtil.getAuthenticationByToken(refreshToken);
        String newAccessToken = tokenGenerator.generateAccessToken(authentication);
        String newRefreshToken = tokenGenerator.generateRefreshToken(authentication);

        return new IssuedToken(newAccessToken, newRefreshToken);
    }
}
