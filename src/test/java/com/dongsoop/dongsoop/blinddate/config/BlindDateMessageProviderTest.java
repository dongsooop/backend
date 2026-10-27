package com.dongsoop.dongsoop.blinddate.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("과팅 진행 메시지 단위 결과")
class BlindDateMessageProviderTest {
    private final BlindDateMessageProvider provider = new BlindDateMessageProvider();

    @ParameterizedTest(name = "B09 요청 수 {0}")
    @ValueSource(ints = {0, 1, 3, 8, 100})
    void requestedEventsAreUniqueAndBounded(int count) {
        var all = provider.getRandomEventMessages(100);
        var selected = provider.getRandomEventMessages(count);
        assertThat(selected).hasSize(Math.min(count, all.size())).doesNotHaveDuplicates();
        assertThat(all).containsAll(selected);
    }

    @Test
    @DisplayName("안내 메시지와 진행자 이름이 비어 있지 않음")
    void introductionIsAvailable() {
        assertThat(provider.getStartMessages())
                .isNotEmpty()
                .allSatisfy(text -> assertThat(text).isNotBlank());
        assertThat(provider.getSessionManagerName()).isNotBlank();
    }
}
