package com.dongsoop.dongsoop.memberdevice.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceResponse;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.exception.AlreadyRegisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.exception.UnauthorizedDeviceAccessException;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberDeviceServiceImpl implements MemberDeviceService {

    private final MemberDeviceRepository memberDeviceRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public void registerDevice(String deviceToken, MemberDeviceType deviceType, Long existingDeviceId) {
        if (existingDeviceId != null) {
            MemberDevice device = memberDeviceRepository.findById(existingDeviceId)
                    .orElseThrow(UnregisteredDeviceException::new);
            validateDuplicateDeviceToken(deviceToken);
            device.updateDeviceToken(deviceToken);
            return;
        }

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
        memberDeviceRepository.save(device);
    }

    // 새로운 WEB 바인딩 메서드: WEB 로그인 흐름에서 디바이스 행을 직접 생성하고 회원을 바인딩한다.
    @Override
    @Transactional
    public Long createAndBindWebDevice(Long memberId, String deviceToken) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        // 토큰이 비어있지 않으면 중복 검증
        if (deviceToken != null && !deviceToken.isBlank()) {
            validateDuplicateDeviceToken(deviceToken);
        }

        MemberDevice memberDevice = MemberDevice.builder()
                .deviceToken(deviceToken)
                .memberDeviceType(MemberDeviceType.WEB)
                .member(member)
                .build();

        memberDeviceRepository.save(memberDevice);
        return memberDevice.getId();
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

    /**
     * FCM 토큰 만료 시 deviceToken을 null로 설정한다.
     *
     * <p>기기 행은 유지하되 토큰만 무효화한다.
     * 이후 알림 발송 쿼리에서 null 토큰은 자동 제외된다.
     *
     * @param deviceToken 무효화할 FCM 토큰
     */
    @Override
    @Transactional
    public void unbindByToken(String deviceToken) {
        memberDeviceRepository.findByDeviceToken(deviceToken)
                .ifPresent(device -> device.updateDeviceToken(null));
    }

    @Override
    public List<MemberDeviceResponse> getDeviceList(Long memberId, String currentDeviceToken) {
        return memberDeviceRepository.findDeviceListByMemberId(memberId, currentDeviceToken);
    }

    @Override
    public String getDeviceTokenIfOwned(Long memberId, Long deviceId) {
        MemberDevice device = memberDeviceRepository.findById(deviceId)
                .orElseThrow(UnregisteredDeviceException::new);

        Member deviceMember = device.getMember();
        if (deviceMember == null || !deviceMember.getId().equals(memberId)) {
            throw new UnauthorizedDeviceAccessException();
        }

        return device.getDeviceToken();
    }

    /**
     * {@inheritDoc} WEB 타입 디바이스는 행 자체를 삭제하고, 모바일은 회원 바인딩만 해제한다.
     */
    @Override
    @Transactional
    public void unbindDevice(Long deviceId) {
        memberDeviceRepository.findById(deviceId).ifPresent(device -> {
            if (device.getMemberDeviceType() == MemberDeviceType.WEB) {
                memberDeviceRepository.delete(device);

                return;
            }

            device.bindMember(null);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Async("memberDeviceLastAccessExecutor")
    @Transactional
    public void updateLastAccessAsync(Long deviceId) {
        memberDeviceRepository.findById(deviceId)
                .ifPresent(device -> device.updateLastAccess(LocalDateTime.now()));
    }

}
