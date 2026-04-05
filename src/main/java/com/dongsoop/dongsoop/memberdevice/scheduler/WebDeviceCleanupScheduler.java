package com.dongsoop.dongsoop.memberdevice.scheduler;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebDeviceCleanupScheduler {

    private final MemberDeviceRepository memberDeviceRepository;

    @Value("${jwt.expired-time.refresh-token}")
    private long refreshTokenExpiryMs;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteExpiredWebDevices() {
        LocalDateTime cutoff = LocalDateTime.now().minusNanos(refreshTokenExpiryMs * 1_000_000L);
        int deleted = memberDeviceRepository.deleteExpiredDevices(MemberDeviceType.WEB, cutoff);
        log.info("Deleted {} expired devices (WEB or null token, cutoff={})", deleted, cutoff);
    }
}
