package com.dongsoop.dongsoop.jwt;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongsoop.dongsoop.jwt.exception.TokenMalformedException;
import com.dongsoop.dongsoop.jwt.exception.TokenRoleNotAvailableException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class JwtMissingSubjectTest {
  private final JwtValidator validator = new JwtValidator();

  @BeforeEach
  void claimNames() {
    ReflectionTestUtils.setField(validator, "roleClaimName", "role");
    ReflectionTestUtils.setField(validator, "typeClaimName", "type");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t", "-7", "7x"})
  void missingBlankOrNonNumericSubjectHasTokenError(String subject) {
    Claims claims =
        Jwts.claims()
            .subject(subject)
            .add("role", List.of("ROLE_USER"))
            .add("type", "ACCESS")
            .build();
    assertThatThrownBy(() -> validator.validate(claims))
        .isInstanceOf(TokenMalformedException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"7", "007", "9223372036854775807"})
  void numericSubjectKeepsExistingContract(String subject) {
    Claims claims =
        Jwts.claims()
            .subject(subject)
            .add("role", List.of("ROLE_USER"))
            .add("type", "ACCESS")
            .build();
    assertThatCode(() -> validator.validate(claims)).doesNotThrowAnyException();
  }

  @Test
  void validSubjectStillRequiresRoles() {
    Claims claims = Jwts.claims().subject("7").add("type", "ACCESS").build();
    assertThatThrownBy(() -> validator.validate(claims))
        .isInstanceOf(TokenRoleNotAvailableException.class);
  }
}
