package com.dongsoop.dongsoop.eclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * 이클래스 토큰 보관에 쓰는 암호화기의 계약을 고정한다.
 *
 * <p>구현은 Spring Security의 것을 그대로 쓰지만, 설정을 잘못 바꿔 평문 저장이나
 * 결정적 암호문으로 퇴화하는 일을 막기 위해 우리가 기대하는 성질을 검증해 둔다.
 */
class EclassTokenEncryptorTest {

    private static final String TOKEN = "ef6067abcdef0123456789abcdef0123";
    private static final String PASSWORD = "dongsoop-local-eclass-token-key";
    private static final String SALT = "5250c5f6fa27b366";

    private TextEncryptor encryptor() {
        return Encryptors.delux(PASSWORD, SALT);
    }

    @Test
    @DisplayName("암호화한 값을 복호화하면 원문이 나온다")
    void roundTrip() {
        TextEncryptor encryptor = encryptor();

        String encrypted = encryptor.encrypt(TOKEN);

        assertThat(encrypted).isNotEqualTo(TOKEN);
        assertThat(encryptor.decrypt(encrypted)).isEqualTo(TOKEN);
    }

    @Test
    @DisplayName("같은 원문도 매번 다른 암호문이 된다")
    void notDeterministic() {
        TextEncryptor encryptor = encryptor();

        assertThat(encryptor.encrypt(TOKEN)).isNotEqualTo(encryptor.encrypt(TOKEN));
    }

    @Test
    @DisplayName("암호문이 컬럼 길이(512) 안에 들어간다")
    void fitsInColumn() {
        assertThat(encryptor().encrypt(TOKEN)).hasSizeLessThan(512);
    }

    @Test
    @DisplayName("키가 다르면 복호화되지 않는다")
    void otherKeyCannotDecrypt() {
        String encrypted = encryptor().encrypt(TOKEN);

        assertThatThrownBy(() -> Encryptors.delux("another-password", SALT).decrypt(encrypted))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("잘린 암호문은 복호화되지 않는다")
    void truncatedCannotDecrypt() {
        TextEncryptor encryptor = encryptor();

        assertThatThrownBy(() -> encryptor.decrypt("zz"))
                .isInstanceOf(RuntimeException.class);
    }
}
