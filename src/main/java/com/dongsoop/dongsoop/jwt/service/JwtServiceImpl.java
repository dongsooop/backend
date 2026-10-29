package com.dongsoop.dongsoop.jwt.service;

import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.jwt.dto.IssuedToken;
import com.dongsoop.dongsoop.jwt.exception.SanctionedMemberReissueException;
import com.dongsoop.dongsoop.report.exception.MemberSanctionedException;
import com.dongsoop.dongsoop.report.validator.ReportValidator;
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
  private final ReportValidator reportValidator;

  public IssuedToken issuedTokenByRefreshToken(String refreshToken) {
    Claims claims = jwtUtil.getClaims(refreshToken);
    jwtValidator.validate(claims);
    jwtValidator.validateRefreshToken(claims);

    // deviceId가 있는 경우 블랙리스트 검사 (없으면 하위 호환을 위해 스킵)
    Long deviceId = claims.get(JwtUtil.DEVICE_ID_CLAIM, Long.class);
    if (deviceId != null) {
      deviceBlacklistService.validateNotBlacklisted(deviceId, JwtUtil.getIssuedAt(claims));
    }

    // 디바이스 ID가 없는 토큰이나 블랙리스트 반영이 실패한 경우에도 정지 회원이 토큰을 이어 받지 못하게 한다.
    // 앱은 재발급이 401일 때만 토큰을 지우고 세션 만료로 처리하므로 로그인과 달리 401로 거절한다
    try {
      reportValidator.checkMemberAccessById(Long.valueOf(claims.getSubject()));
    } catch (MemberSanctionedException e) {
      throw new SanctionedMemberReissueException(e);
    }

    Authentication authentication = jwtUtil.getAuthenticationByToken(refreshToken);
    String newAccessToken = tokenGenerator.generateAccessToken(authentication, deviceId);
    String newRefreshToken = tokenGenerator.generateRefreshToken(authentication, deviceId);

    return new IssuedToken(newAccessToken, newRefreshToken);
  }
}
