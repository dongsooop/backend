package com.dongsoop.dongsoop.memberdevice.service;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * FID 또는 FCM 디바이스 토큰으로 비회원 디바이스를 해석한다.
 *
 * <p>fid가 있으면 우선 사용한다 — deviceToken은 로테이션되지만 fid는 안정적이라,
 * fid로 식별해야 토큰 갱신 후에도 같은 기기로 인식된다. fid가 없거나 아직 그 기기에
 * 채워지지 않은 전환 기간에는 deviceToken으로 폴백한다.
 *
 * <p>회원에 바인딩된 디바이스는 비회원 설정의 주체가 아니므로 거부한다.
 * 회원 전환 이후 비회원 설정이 더 이상 노출되지 않게 하는 지점이다.
 */
@Component
@RequiredArgsConstructor
public class GuestDeviceResolver {

    private final MemberDeviceRepository memberDeviceRepository;

    public MemberDevice resolve(String fid, String deviceToken) {
        if (StringUtils.hasText(fid)) {
            Optional<MemberDevice> device = memberDeviceRepository.findByFid(fid);
            if (device.isPresent()) {
                return validateGuestOwned(device.get());
            }
        }

        if (!StringUtils.hasText(deviceToken)) {
            throw new UnregisteredDeviceException();
        }

        MemberDevice device = memberDeviceRepository.findByDeviceToken(deviceToken)
                .orElseThrow(UnregisteredDeviceException::new);

        return validateGuestOwned(device);
    }

    private MemberDevice validateGuestOwned(MemberDevice device) {
        if (device.getMember() != null) {
            throw new UnregisteredDeviceException();
        }

        return device;
    }
}
