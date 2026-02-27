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
    private final DeviceBlacklistService deviceBlacklistService;

    public IssuedToken issuedTokenByRefreshToken(String refreshToken) {
        Claims claims = jwtUtil.getClaims(refreshToken);
        jwtValidator.validate(claims);
        jwtValidator.validateRefreshToken(claims);

        // deviceId가 있는 경우 블랙리스트 검사 (없으면 하위 호환을 위해 스킵)
        Long deviceId = claims.get(JwtUtil.DEVICE_ID_CLAIM, Long.class);
        if (deviceId != null) {
            deviceBlacklistService.validateNotBlacklisted(deviceId, claims.getIssuedAt());
        }

        Authentication authentication = jwtUtil.getAuthenticationByToken(refreshToken);
        String newAccessToken = tokenGenerator.generateAccessToken(authentication, deviceId);
        String newRefreshToken = tokenGenerator.generateRefreshToken(authentication, deviceId);

        return new IssuedToken(newAccessToken, newRefreshToken);
    }
}
