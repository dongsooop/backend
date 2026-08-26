package com.dongsoop.dongsoop.memberdevice.scheduler;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 익명 키 도입 이전에 등록된 비회원 디바이스에 키를 채운다.
 *
 * <p>{@code ddl-auto: update} 는 컬럼만 추가하고 값을 채우지 않으므로 부팅 시 1회 수행한다.
 * 대상이 없으면 아무 것도 하지 않으며, 재실행해도 안전하다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnonymousKeyBackfillRunner implements ApplicationRunner {

    private final MemberDeviceRepository memberDeviceRepository;

    @Override
    public void run(ApplicationArguments args) {
        backfill();
    }

    @Transactional
    public int backfill() {
        List<MemberDevice> targets = memberDeviceRepository.findByMemberIsNullAndAnonymousKeyIsNull();
        if (targets.isEmpty()) {
            return 0;
        }

        targets.forEach(MemberDevice::issueAnonymousKeyIfAbsent);
        log.info("Backfilled anonymous keys for {} guest devices", targets.size());

        return targets.size();
    }
}
