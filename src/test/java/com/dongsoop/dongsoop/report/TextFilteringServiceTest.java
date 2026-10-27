package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.report.service.TextFilteringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TextFilteringServiceTest {

    @Test
    @DisplayName("필터 API 호출이 실패하면 예외를 던지지 않고 욕설 없음으로 돌려준다")
    void hasProfanity_ApiUnreachable_ReturnsFalse() {
        TextFilteringService service = new TextFilteringService();
        ReflectionTestUtils.setField(service, "filteringApiUrl", "http://127.0.0.1:1/filter");
        ReflectionTestUtils.setField(service, "jwtSecretKey", "key");

        assertThat(service.hasProfanity("제목", "", "본문")).isFalse();
    }
}
