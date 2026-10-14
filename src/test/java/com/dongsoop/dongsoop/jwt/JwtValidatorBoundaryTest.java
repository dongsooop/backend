package com.dongsoop.dongsoop.jwt;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongsoop.dongsoop.jwt.exception.NotAccessTokenException;
import com.dongsoop.dongsoop.jwt.exception.NotRefreshTokenException;
import com.dongsoop.dongsoop.jwt.exception.TokenMalformedException;
import com.dongsoop.dongsoop.jwt.exception.TokenRoleNotAvailableException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class JwtValidatorBoundaryTest {
    private final JwtValidator validator = new JwtValidator();

    @BeforeEach
    void configureClaimNames() {
        ReflectionTestUtils.setField(validator, "roleClaimName", "role");
        ReflectionTestUtils.setField(validator, "typeClaimName", "type");
    }

    @ParameterizedTest
    @EnumSource(JWTType.class)
    void acceptsWellFormedToken(JWTType type) {
        Claims claims = claims("7", List.of("ROLE_USER"), type.name());
        assertThatCode(() -> validator.validate(claims)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"anonymousUser", "-7", "7x"})
    void rejectsNonNumericSubject(String subject) {
        Claims claims = claims(subject, List.of("ROLE_USER"), "ACCESS");
        assertThatThrownBy(() -> validator.validate(claims)).isInstanceOf(TokenMalformedException.class);
    }

    @Test
    void rejectsAbsentRoles() {
        Claims claims = claims("7", null, "ACCESS");
        assertThatThrownBy(() -> validator.validate(claims)).isInstanceOf(TokenRoleNotAvailableException.class);
    }

    @Test
    void rejectsEmptyRoles() {
        Claims claims = claims("7", List.of(), "ACCESS");
        assertThatThrownBy(() -> validator.validate(claims)).isInstanceOf(TokenRoleNotAvailableException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "access", "UNKNOWN"})
    void rejectsMissingOrUnknownType(String type) {
        Claims claims = claims("7", List.of("ROLE_USER"), type);
        assertThatThrownBy(() -> validator.validate(claims)).isInstanceOf(TokenMalformedException.class);
        assertThatThrownBy(() -> validator.validateAccessToken(claims)).isInstanceOf(TokenMalformedException.class);
        assertThatThrownBy(() -> validator.validateRefreshToken(claims)).isInstanceOf(TokenMalformedException.class);
    }

    @ParameterizedTest
    @EnumSource(value = JWTType.class, names = {"REFRESH", "SOCIAL"})
    void rejectsOtherTypesAsAccess(JWTType type) {
        Claims claims = claims("7", List.of("ROLE_USER"), type.name());
        assertThatThrownBy(() -> validator.validateAccessToken(claims)).isInstanceOf(NotAccessTokenException.class);
    }

    @ParameterizedTest
    @EnumSource(value = JWTType.class, names = {"ACCESS", "SOCIAL"})
    void rejectsOtherTypesAsRefresh(JWTType type) {
        Claims claims = claims("7", List.of("ROLE_USER"), type.name());
        assertThatThrownBy(() -> validator.validateRefreshToken(claims)).isInstanceOf(NotRefreshTokenException.class);
    }

    @Test
    void acceptsMatchingAccessAndRefreshTypes() {
        Claims access = claims("7", List.of("ROLE_USER"), "ACCESS");
        Claims refresh = claims("7", List.of("ROLE_USER"), "REFRESH");
        assertThatCode(() -> validator.validateAccessToken(access)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateRefreshToken(refresh)).doesNotThrowAnyException();
    }

    private Claims claims(String subject, List<String> roles, String type) {
        return Jwts.claims().subject(subject).add("role", roles).add("type", type).build();
    }
}
