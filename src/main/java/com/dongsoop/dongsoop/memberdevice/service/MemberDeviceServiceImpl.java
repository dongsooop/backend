package com.dongsoop.dongsoop.memberdevice.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.exception.AlreadyRegisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notification.service.FCMService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberDeviceServiceImpl implements MemberDeviceService {

    private final MemberDeviceRepository memberDeviceRepository;
    private final FCMService fcmService;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${notification.topic.anonymous}")
    private String anonymousTopic;

    @Override
    public void registerDevice(String deviceToken, MemberDeviceType deviceType) {
        validateDuplicateDeviceToken(deviceToken);

        MemberDevice memberDevice = MemberDevice.builder()
                .deviceToken(deviceToken)
                .memberDeviceType(deviceType)
                .build();

        memberDeviceRepository.save(memberDevice);
    }

    @Override
    @Transactional
    public void bindDeviceWithMemberId(Long memberId, String deviceToken) {
        MemberDevice device = memberDeviceRepository.findByDeviceToken(deviceToken)
                .orElseThrow(UnregisteredDeviceException::new);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        device.bindMember(member);
    }

    private void validateDuplicateDeviceToken(String deviceToken) {
        if (memberDeviceRepository.existsByDeviceToken(deviceToken)) {
            throw new AlreadyRegisteredDeviceException();
        }
    }

    /**
     * MemberId로 MemberDevice 조회
     *
     * @param memberId MemberId List
     * @return MemberId를 key로, deviceToken List를 value로 갖는 Map
     */
    @Override
    public List<String> getDeviceByMemberId(Long memberId) {
        return memberDeviceRepository.getDeviceByMemberId(memberId);
    }

    /**
     * MemberId List로 MemberDevice 조회
     *
     * @param condition 알림을 보낼 사용자 목록과 알림 타입
     * @return MemberId를 key로, deviceToken List를 value로 갖는 Map
     */
    @Override
    public Map<Long, List<String>> getDeviceByMember(MemberDeviceFindCondition condition) {
        List<MemberDeviceDto> memberDeviceDtos = memberDeviceRepository.findDevicesWithNotification(condition);

        return memberDeviceDtos.stream()
                .collect(deviceGroupByMemberId());
    }

    /**
     * MemberDevice에 대해 MemberId로 그룹화
     *
     * @return MemberId를 key로, deviceToken List를 value로 갖는 Map
     */
    private Collector<MemberDeviceDto, ?, Map<Long, List<String>>> deviceGroupByMemberId() {
        return Collectors.groupingBy(
                memberDeviceDto -> memberDeviceDto.member().getId(),
                Collectors.mapping(MemberDeviceDto::deviceToken, Collectors.toList()));
    }

    @Override
    @Transactional
    public void deleteByToken(String deviceToken) {
        memberDeviceRepository.deleteByDeviceToken(deviceToken);
    }
}
