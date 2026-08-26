package com.dongsoop.dongsoop.memberdevice;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberDeviceAnonymousKeyTest {

    @Test
    @DisplayName("익명 키가 없으면 새로 발급한다")
    void issues_key_when_absent() {
        MemberDevice device = MemberDevice.builder()
                .deviceToken("token-a")
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();

        String issued = device.issueAnonymousKeyIfAbsent();

        assertThat(issued).isNotBlank();
        assertThat(device.getAnonymousKey()).isEqualTo(issued);
    }

    @Test
    @DisplayName("익명 키가 이미 있으면 기존 값을 그대로 반환한다")
    void keeps_existing_key() {
        MemberDevice device = MemberDevice.builder()
                .deviceToken("token-a")
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();

        String first = device.issueAnonymousKeyIfAbsent();
        String second = device.issueAnonymousKeyIfAbsent();

        assertThat(second).isEqualTo(first);
    }
}
