package com.dongsoop.dongsoop.mailverify;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.mailverify.exception.MissingMailVerifyException;
import com.dongsoop.dongsoop.mailverify.exception.UsingAllMailVerifyOpportunityException;
import com.dongsoop.dongsoop.mailverify.exception.VerifyMailCodeNotAvailableException;
import com.dongsoop.dongsoop.mailverify.passwordupdate.PasswordUpdateMailValidator;
import com.dongsoop.dongsoop.mailverify.register.RegisterMailValidator;
import com.dongsoop.dongsoop.mailverify.service.MailVerifier;
import com.dongsoop.dongsoop.mailverify.service.MailVerifyNumberGenerator;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class MailVerifierTest {
    @Mock private RedisTemplate<String, Object> redis;
    @Mock private HashOperations<String, Object, Object> hash;
    @Mock private MailVerifyNumberGenerator generator;
    private static final String EMAIL = "student@example.com";
    private static final String REDIS_KEY = "verification:test-key";

    enum Purpose { REGISTER, PASSWORD_UPDATE }

    @ParameterizedTest
    @EnumSource(Purpose.class)
    @DisplayName("회원가입과 비밀번호 변경의 올바른 코드 인증은 성공 상태와 10분 만료를 저장한다")
    void validCodeMarksSuccessWithExpiry(Purpose purpose) {
        MailVerifier verifier = verifier(purpose);
        stubKey(purpose);
        when(redis.opsForHash()).thenReturn(hash);
        when(hash.get(REDIS_KEY, "opportunity")).thenReturn(3);
        when(hash.get(REDIS_KEY, "code")).thenReturn("ABC123");

        verifier.validateVerificationCodeWithOpportunity(EMAIL, "ABC123");

        verify(hash).put(REDIS_KEY, "success", true);
        verify(redis).expire(REDIS_KEY, Duration.ofMinutes(10));
        verify(hash, never()).increment(anyString(), any(), anyLong());
    }

    @ParameterizedTest
    @MethodSource("invalidCodes")
    @DisplayName("잘못된 코드나 누락·타입 불일치는 인증 기회를 한 번 차감하고 성공으로 표시하지 않는다")
    void invalidCodeConsumesOneOpportunity(Object storedCode) {
        stubKey(Purpose.REGISTER);
        when(redis.opsForHash()).thenReturn(hash);
        when(hash.get(REDIS_KEY, "opportunity")).thenReturn(3);
        when(hash.get(REDIS_KEY, "code")).thenReturn(storedCode);

        assertThrows(VerifyMailCodeNotAvailableException.class,
                () -> verifier(Purpose.REGISTER).validateVerificationCodeWithOpportunity(EMAIL, "ABC123"));

        verify(hash).increment(REDIS_KEY, "opportunity", -1L);
        verify(hash, never()).put(anyString(), any(), any());
        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    static Stream<Object> invalidCodes() {
        return Stream.of(null, "WRONG", 123456);
    }

    @ParameterizedTest
    @MethodSource("invalidOpportunities")
    @DisplayName("기회가 없거나 소진되었거나 타입이 잘못되면 코드 비교와 추가 차감 없이 거절한다")
    void exhaustedOpportunityStopsVerification(Object opportunity) {
        stubKey(Purpose.REGISTER);
        when(redis.opsForHash()).thenReturn(hash);
        when(hash.get(REDIS_KEY, "opportunity")).thenReturn(opportunity);

        assertThrows(UsingAllMailVerifyOpportunityException.class,
                () -> verifier(Purpose.REGISTER).validateVerificationCodeWithOpportunity(EMAIL, "ABC123"));

        verify(hash, never()).get(REDIS_KEY, "code");
        verify(hash, never()).increment(anyString(), any(), anyLong());
        verify(hash, never()).put(anyString(), any(), any());
        verify(redis, never()).expire(anyString(), any(Duration.class));
    }

    static Stream<Object> invalidOpportunities() {
        return Stream.of(null, 0, -1, "3", 3L);
    }

    @ParameterizedTest
    @MethodSource("invalidSuccessFlags")
    @DisplayName("인증 성공 상태가 없거나 false 또는 다른 타입이면 가입·변경을 허용하지 않는다")
    void rejectsUnverifiedState(Object success) {
        stubKey(Purpose.REGISTER);
        when(redis.opsForHash()).thenReturn(hash);
        when(hash.get(REDIS_KEY, "success")).thenReturn(success);

        assertThrows(MissingMailVerifyException.class,
                () -> verifier(Purpose.REGISTER).validateVerifySuccess(EMAIL));
    }

    static Stream<Object> invalidSuccessFlags() {
        return Stream.of(null, false, "true", 1);
    }

    @Test
    @DisplayName("인증 성공 상태가 true이면 검증을 통과한다")
    void acceptsVerifiedState() {
        stubKey(Purpose.REGISTER);
        when(redis.opsForHash()).thenReturn(hash);
        when(hash.get(REDIS_KEY, "success")).thenReturn(true);
        assertDoesNotThrow(() -> verifier(Purpose.REGISTER).validateVerifySuccess(EMAIL));
    }

    @ParameterizedTest
    @EnumSource(Purpose.class)
    @DisplayName("인증 삭제는 회원가입과 비밀번호 변경 각각의 해시 키를 삭제한다")
    void deletesPurposeSpecificCode(Purpose purpose) {
        stubKey(purpose);
        verifier(purpose).removeVerificationCode(EMAIL);
        verify(redis).delete(REDIS_KEY);
    }

    private MailVerifier verifier(Purpose purpose) {
        if (purpose == Purpose.REGISTER) {
            return new RegisterMailValidator(redis, generator, "opportunity", "code", "success", "register:");
        }
        return new PasswordUpdateMailValidator(redis, generator, "opportunity", "code", "success", "password:");
    }

    private void stubKey(Purpose purpose) {
        if (purpose == Purpose.REGISTER) {
            when(generator.getRedisKeyByHashed("register:", EMAIL)).thenReturn(REDIS_KEY);
            return;
        }
        when(generator.getRedisKeyByHashed("password:", EMAIL)).thenReturn(REDIS_KEY);
    }
}
