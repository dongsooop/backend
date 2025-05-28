package com.dongsoop.dongsoop.jwt.service;

import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.jwt.dto.IssuedToken;
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
        jwtValidator.validate(refreshToken);

        Authentication authentication = jwtUtil.getAuthenticationByToken(refreshToken);
        String newAccessToken = tokenGenerator.generateAccessToken(authentication);
        String newRefreshToken = tokenGenerator.generateRefreshToken(authentication);

        return new IssuedToken(newAccessToken, newRefreshToken);
    }
}
