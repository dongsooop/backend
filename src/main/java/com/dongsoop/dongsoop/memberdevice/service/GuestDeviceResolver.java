package com.dongsoop.dongsoop.memberdevice.service;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * FCM 디바이스 토큰으로 비회원 디바이스를 해석한다.
 *
 * <p>회원에 바인딩된 디바이스는 비회원 설정의 주체가 아니므로 거부한다.
 * 회원 전환 이후 비회원 설정이 더 이상 노출되지 않게 하는 지점이다.
 */
@Component
@RequiredArgsConstructor
public class GuestDeviceResolver {

    private final MemberDeviceRepository memberDeviceRepository;

    public MemberDevice resolve(String deviceToken) {
        if (!StringUtils.hasText(deviceToken)) {
            throw new UnregisteredDeviceException();
        }

        MemberDevice device = memberDeviceRepository.findByDeviceToken(deviceToken)
                .orElseThrow(UnregisteredDeviceException::new);

        if (device.getMember() != null) {
            throw new UnregisteredDeviceException();
        }

        return device;
    }
}
