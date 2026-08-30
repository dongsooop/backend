package com.dongsoop.dongsoop.memberdevice.service;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * FID 또는 FCM 디바이스 토큰으로 디바이스를 해석한다.
 *
 * <p>fid가 있으면 우선 사용한다 — deviceToken은 로테이션되지만 fid는 안정적이라,
 * fid로 식별해야 토큰 갱신 후에도 같은 기기로 인식된다. fid가 없거나 아직 그 기기에
 * 채워지지 않은 전환 기간에는 deviceToken으로 폴백한다.
 *
 * <p>회원/비회원 구분 없이 디바이스 단위로 해석한다 — 구독 학과 설정은 회원 여부와
 * 무관하게 디바이스 단위로 관리된다.
 */
@Component
@RequiredArgsConstructor
public class GuestDeviceResolver {

    private final MemberDeviceRepository memberDeviceRepository;

    public MemberDevice resolve(String fid, String deviceToken) {
        if (StringUtils.hasText(fid)) {
            Optional<MemberDevice> device = memberDeviceRepository.findByFid(fid);
            if (device.isPresent()) {
                return device.get();
            }
        }

        if (!StringUtils.hasText(deviceToken)) {
            throw new UnregisteredDeviceException();
        }

        return memberDeviceRepository.findByDeviceToken(deviceToken)
                .orElseThrow(UnregisteredDeviceException::new);
    }
}
