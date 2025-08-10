package com.dongsoop.dongsoop.memberdevice.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.exception.AlreadyRegisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberDeviceServiceImpl implements MemberDeviceService {

    private final MemberRepository memberRepository;
    private final MemberDeviceRepository memberDeviceRepository;

    @Override
    public void registerDeviceByMemberId(Long memberId, String deviceToken, MemberDeviceType deviceType) {
        validateDuplicateDeviceToken(deviceToken);

        Member member = memberRepository.getReferenceById(memberId);
        MemberDevice memberDevice = MemberDevice.builder()
                .member(member)
                .deviceToken(deviceToken)
                .memberDeviceType(deviceType)
                .build();

        memberDeviceRepository.save(memberDevice);
    }

    private void validateDuplicateDeviceToken(String deviceToken) {
        if (memberDeviceRepository.existsByDeviceToken(deviceToken)) {
            throw new AlreadyRegisteredDeviceException();
        }
    }
}
