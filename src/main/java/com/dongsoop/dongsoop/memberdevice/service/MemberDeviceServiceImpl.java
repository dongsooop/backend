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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberDeviceServiceImpl implements MemberDeviceService {

    private final MemberDeviceRepository memberDeviceRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public void registerDevice(String deviceToken, String fid, MemberDeviceType deviceType, Long existingDeviceId) {
        if (existingDeviceId != null) {
            MemberDevice device = memberDeviceRepository.findById(existingDeviceId)
                    .orElseThrow(UnregisteredDeviceException::new);
            validateDuplicateDeviceToken(deviceToken);
            device.updateDeviceToken(deviceToken);
            updateFidIfPresent(device, fid);
            return;
        }

        // 비회원: fid가 있으면 그걸로 먼저 찾는다. deviceToken이 로테이션돼도 같은 기기로 인식되어
        // device_notice_preference 구독이 끊기지 않는다.
        if (StringUtils.hasText(fid)) {
            Optional<MemberDevice> byFid = memberDeviceRepository.findByFid(fid);
            if (byFid.isPresent()) {
                MemberDevice device = byFid.get();
                if (device.getMember() != null) {
                    throw new AlreadyRegisteredDeviceException();
                }
                device.updateDeviceToken(deviceToken);
                return;
            }
        }

        // fid로 못 찾았으면 deviceToken으로 찾는다. fid 컬럼 도입 이전부터 있던 기존 기기일 수 있으므로,
        // 여기서 새 행을 만들면 기존 device_notice_preference가 고아가 된다 — 반드시 기존 행에 fid를 채운다(백필).
        Optional<MemberDevice> existing = memberDeviceRepository.findByDeviceToken(deviceToken);
        if (existing.isPresent()) {
            MemberDevice device = existing.get();
            if (device.getMember() != null) {
                throw new AlreadyRegisteredDeviceException();
            }
            updateFidIfPresent(device, fid);
            return;
        }

        MemberDevice memberDevice = MemberDevice.builder()
                .deviceToken(deviceToken)
                .fid(fid)
                .memberDeviceType(deviceType)
                .build();
        memberDeviceRepository.save(memberDevice);
    }

    private void updateFidIfPresent(MemberDevice device, String fid) {
        if (StringUtils.hasText(fid)) {
            device.updateFid(fid);
        }
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
    // deviceToken이 없으면 UUID를 생성하여 사용하고, 실제로 사용된 토큰을 반환한다.
    @Override
    @Transactional
    public String createAndBindWebDevice(Long memberId, String deviceToken) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        String effectiveToken = (deviceToken != null && !deviceToken.isBlank())
                ? deviceToken
                : UUID.randomUUID().toString();

        validateDuplicateDeviceToken(effectiveToken);

        MemberDevice memberDevice = MemberDevice.builder()
                .deviceToken(effectiveToken)
                .memberDeviceType(MemberDeviceType.WEB)
                .member(member)
                .build();

        memberDeviceRepository.save(memberDevice);
        return effectiveToken;
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
