package com.dongsoop.dongsoop.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AesGcmEncryptorTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    @DisplayName("암호화한 값을 복호화하면 원문이 나온다")
    void roundTrip() {
        AesGcmEncryptor encryptor = new AesGcmEncryptor(KEY);

        String encrypted = encryptor.encrypt("ef6067abcdef");

        assertThat(encrypted).isNotEqualTo("ef6067abcdef");
        assertThat(encryptor.decrypt(encrypted)).isEqualTo("ef6067abcdef");
    }

    @Test
    @DisplayName("같은 원문도 매번 다른 암호문이 나온다 (IV 랜덤)")
    void randomIv() {
        AesGcmEncryptor encryptor = new AesGcmEncryptor(KEY);

        assertThat(encryptor.encrypt("token")).isNotEqualTo(encryptor.encrypt("token"));
    }

    @Test
    @DisplayName("키 길이가 32바이트가 아니면 생성 시 실패한다")
    void invalidKey() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new AesGcmEncryptor(shortKey)).isInstanceOf(IllegalArgumentException.class);
    }
}
