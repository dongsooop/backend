package com.dongsoop.dongsoop.memberdevice;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.memberdevice.scheduler.AnonymousKeyBackfillRunner;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class AnonymousKeyBackfillStartupTest {

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
    @DisplayName("ApplicationRunner 진입점으로 실행하면 익명 키가 DB에 반영된다")
    void persists_when_invoked_through_runner_entry_point() {
        MemberDevice legacy = memberDeviceRepository.save(MemberDevice.builder()
                .deviceToken("token-legacy-runner")
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build());
        Long id = legacy.getId();

        runner.run(new DefaultApplicationArguments());

        assertThat(memberDeviceRepository.findById(id))
                .get()
                .extracting(MemberDevice::getAnonymousKey)
                .isNotNull();
    }

    @AfterEach
    void cleanup() {
        memberDeviceRepository.findByDeviceToken("token-legacy-runner")
                .ifPresent(memberDeviceRepository::delete);
    }
}
