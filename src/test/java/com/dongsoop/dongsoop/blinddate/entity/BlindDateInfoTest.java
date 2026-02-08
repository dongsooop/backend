package com.dongsoop.dongsoop.blinddate.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * BlindDateInfo Repository 단위 테스트 BlindDateInfo는 빈 Entity이고, 실제 로직은 BlindDateInfoRepositoryImpl에 있음 Repository 테스트는
 * BlindDateInfoRepositoryTest에서 수행
 */
@DisplayName("BlindDateInfo Entity 단위 테스트")
class BlindDateInfoTest {

    @Test
    @DisplayName("BlindDateInfo 객체 생성 가능")
    void createBlindDateInfo() {
        // when
        BlindDateInfo info = new BlindDateInfo();

        // then
        assertThat(info).isNotNull();
    }
}
