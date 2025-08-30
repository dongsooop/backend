package com.dongsoop.dongsoop.memberdevice.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.exception.AlreadyRegisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberDeviceServiceImpl implements MemberDeviceService {

    private final MemberDeviceRepository memberDeviceRepository;
    private final MemberRepository memberRepository;

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
     * MemberId List로 MemberDevice 조회
     *
     * @param memberIdList MemberId List
     * @return MemberId를 key로, deviceToken List를 value로 갖는 Map
     */
    @Override
    @Cacheable(value = "deviceTokens", key = "#memberIdList", sync = true)
    public Map<Long, List<String>> getDeviceByMember(List<Long> memberIdList) {
        List<MemberDeviceDto> memberDeviceDtos = memberDeviceRepository.getMemberDeviceTokenByMemberIds(memberIdList);

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

    /**
     * 캐시 삭제 (평일 8시~19시 9분 간격 공지사항 파싱 전 실시간 반영 목적)
     */
    @Scheduled(cron = "0 */9 8-19 * * MON-FRI", zone = "Asia/Seoul")
    @CacheEvict(value = "deviceTokens", allEntries = true)
    public void deleteData() {
        log.info("Cache 'deviceTokens' cleared");
    }
}
