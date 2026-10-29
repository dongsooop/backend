package com.dongsoop.dongsoop.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.jwt.exception.BlacklistedTokenException;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeviceBlacklistBoundaryTest {
  @Mock StringRedisTemplate redis;
  @Mock ValueOperations<String, String> values;
  @InjectMocks DeviceBlacklistService service;

  @Test
  void missingIssuedAtSkipsRedis() {
    service.validateNotBlacklisted(7L, null);
    verifyNoInteractions(redis, values);
  }

  @Test
  void absentBlacklistAllowsToken() {
    when(redis.opsForValue()).thenReturn(values);
    Instant issuedAt = Instant.ofEpochMilli(1000);
    assertThatCode(() -> service.validateNotBlacklisted(7L, issuedAt)).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(longs = {999, 1000})
  void tokenIssuedAtOrBeforeBlacklistIsRejected(long epochMillis) {
    when(redis.opsForValue()).thenReturn(values);
    when(values.get("device:blacklist:7")).thenReturn("1000");
    Instant issuedAt = Instant.ofEpochMilli(epochMillis);
    assertThatThrownBy(() -> service.validateNotBlacklisted(7L, issuedAt))
        .isInstanceOf(BlacklistedTokenException.class);
  }

  @Test
  void tokenIssuedAfterBlacklistIsAllowed() {
    when(redis.opsForValue()).thenReturn(values);
    when(values.get("device:blacklist:7")).thenReturn("1000");
    Instant issuedAt = Instant.ofEpochMilli(1001);
    assertThatCode(() -> service.validateNotBlacklisted(7L, issuedAt)).doesNotThrowAnyException();
  }

  @Test
  void storesDeviceKeyCurrentTimestampAndRefreshLifetime() {
    when(redis.opsForValue()).thenReturn(values);
    ReflectionTestUtils.setField(service, "refreshTokenExpiredTime", 60000L);
    long before = System.currentTimeMillis();
    service.blacklist(7L);
    ArgumentCaptor<String> timestamp = ArgumentCaptor.forClass(String.class);
    verify(values).set(eq("device:blacklist:7"), timestamp.capture(), eq(Duration.ofMinutes(1)));
    assertThat(Long.parseLong(timestamp.getValue())).isBetween(before, System.currentTimeMillis());
  }
}
