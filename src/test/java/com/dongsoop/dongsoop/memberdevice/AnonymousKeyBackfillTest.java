package com.dongsoop.dongsoop.memberdevice;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.memberdevice.scheduler.AnonymousKeyBackfillRunner;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnonymousKeyBackfillTest {

    @Autowired
    private AnonymousKeyBackfillRunner runner;

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    @Test
    @DisplayName("익명 키가 없는 비회원 디바이스에 키를 채운다")
    void fills_missing_keys() {
        MemberDevice legacy = memberDeviceRepository.save(MemberDevice.builder()
                .deviceToken("token-legacy-1")
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build());

        int filled = runner.backfill();

        assertThat(filled).isEqualTo(1);
        assertThat(memberDeviceRepository.findById(legacy.getId()))
                .get()
                .extracting(MemberDevice::getAnonymousKey)
                .isNotNull();
    }

    @Test
    @DisplayName("두 번 실행해도 두 번째에는 채울 대상이 없다")
    void is_idempotent() {
        memberDeviceRepository.save(MemberDevice.builder()
                .deviceToken("token-legacy-2")
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build());

        runner.backfill();
        int second = runner.backfill();

        assertThat(second).isZero();
    }
}
