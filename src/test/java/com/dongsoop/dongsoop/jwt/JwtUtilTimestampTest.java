package com.dongsoop.dongsoop.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTimestampTest {
  @Test
  void missingIssuedAtRemainsNull() {
    Claims claims = Jwts.claims().build();
    assertThat(JwtUtil.getIssuedAt(claims)).isNull();
  }

  @Test
  void readsIssuedAtAsInstant() {
    Instant issuedAt = Instant.parse("2026-10-14T09:00:00Z");
    Claims claims = Jwts.claims().add("iat", issuedAt.getEpochSecond()).build();
    assertThat(JwtUtil.getIssuedAt(claims)).isEqualTo(issuedAt);
  }

  @Test
  void issuedTokenPreservesExpirationAndClaims() {
    SecretKey key = Jwts.SIG.HS256.key().build();
    JwtKeyManager keyManager = mock(JwtKeyManager.class);
    when(keyManager.getSecretKey()).thenReturn(key);
    JwtUtil util = new JwtUtil(keyManager);
    ReflectionTestUtils.setField(util, "roleClaimName", "role");
    ReflectionTestUtils.setField(util, "typeClaimName", "type");
    Instant before = Instant.now();
    Instant expiresAt = before.plusSeconds(600);

    String token = util.issue(expiresAt, "7", List.of("ROLE_USER"), JWTType.ACCESS, 3L);
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

    assertThat(claims.getSubject()).isEqualTo("7");
    assertThat(claims.get("exp", Long.class)).isEqualTo(expiresAt.getEpochSecond());
    assertThat(JwtUtil.getIssuedAt(claims).getEpochSecond())
        .isBetween(before.getEpochSecond(), Instant.now().getEpochSecond());
    assertThat(claims.get("type", String.class)).isEqualTo("ACCESS");
    assertThat(claims.get(JwtUtil.DEVICE_ID_CLAIM, Long.class)).isEqualTo(3L);
  }
}
