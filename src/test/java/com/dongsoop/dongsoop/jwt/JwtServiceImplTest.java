package com.dongsoop.dongsoop.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.common.exception.CustomException;
import com.dongsoop.dongsoop.jwt.dto.IssuedToken;
import com.dongsoop.dongsoop.jwt.exception.SanctionedMemberReissueException;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import com.dongsoop.dongsoop.jwt.service.JwtServiceImpl;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.exception.MemberSanctionedException;
import com.dongsoop.dongsoop.report.validator.ReportValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {

  @Mock private JwtValidator jwtValidator;
  @Mock private TokenGenerator tokenGenerator;
  @Mock private JwtUtil jwtUtil;
  @Mock private DeviceBlacklistService deviceBlacklistService;
  @Mock private ReportValidator reportValidator;
  @Mock private Authentication authentication;
  @InjectMocks private JwtServiceImpl jwtService;

  @Test
  void suspendedMemberWithoutDeviceIdCannotReissue() {
    Claims claims = Jwts.claims().subject("7").build();
    when(jwtUtil.getClaims("refresh")).thenReturn(claims);
    doThrow(
            new MemberSanctionedException(
                Sanction.builder().sanctionType(SanctionType.TEMPORARY_BAN).build()))
        .when(reportValidator)
        .checkMemberAccessById(7L);

    assertThatThrownBy(() -> jwtService.issuedTokenByRefreshToken("refresh"))
        .isInstanceOf(SanctionedMemberReissueException.class)
        .hasMessage("회원님은 현재 제재 중입니다. 자세한 내용은 고객센터에 문의해주세요.")
        .extracting(e -> ((CustomException) e).getHttpStatus())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(tokenGenerator, never()).generateAccessToken(any(), any());
    verify(tokenGenerator, never()).generateRefreshToken(any(), any());
  }

  @Test
  void memberWithoutActiveBanReissuesTokens() {
    Claims claims = Jwts.claims().subject("7").build();
    when(jwtUtil.getClaims("refresh")).thenReturn(claims);
    when(jwtUtil.getAuthenticationByToken("refresh")).thenReturn(authentication);
    when(tokenGenerator.generateAccessToken(authentication, null)).thenReturn("access");
    when(tokenGenerator.generateRefreshToken(authentication, null)).thenReturn("new-refresh");

    IssuedToken issued = jwtService.issuedTokenByRefreshToken("refresh");

    assertThat(issued.getAccessToken()).isEqualTo("access");
    assertThat(issued.getRefreshToken()).isEqualTo("new-refresh");
    verify(reportValidator).checkMemberAccessById(7L);
  }
}
