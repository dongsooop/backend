package com.dongsoop.dongsoop.eclass.service;

import com.dongsoop.dongsoop.common.crypto.AesGcmEncryptor;
import com.dongsoop.dongsoop.eclass.client.EclassClient;
import com.dongsoop.dongsoop.eclass.client.dto.MoodleSiteInfoResponse;
import com.dongsoop.dongsoop.eclass.dto.EclassLinkResponse;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.eclass.exception.EclassLinkNotFoundException;
import com.dongsoop.dongsoop.eclass.exception.EclassSyncCooldownException;
import com.dongsoop.dongsoop.eclass.repository.EclassAssignmentRepository;
import com.dongsoop.dongsoop.eclass.repository.EclassLinkRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.service.NoticePreferenceDeviceResolver;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EclassLinkServiceImpl implements EclassLinkService {

    private final NoticePreferenceDeviceResolver deviceResolver;
    private final EclassLinkRepository linkRepository;
    private final EclassAssignmentRepository assignmentRepository;
    private final EclassClient eclassClient;
    private final AesGcmEncryptor encryptor;
    private final EclassSyncService syncService;
    private final Clock clock;

    @Value("${eclass.sync.manual-cooldown-seconds}")
    private long manualCooldownSeconds;

    @Override
    @Transactional
    public EclassLinkResponse link(String fid, String deviceToken, String moodleToken) {
        MemberDevice device = deviceResolver.resolve(fid, deviceToken);
        MoodleSiteInfoResponse info = eclassClient.getSiteInfo(moodleToken);

        String encrypted = encryptor.encrypt(moodleToken);
        LocalDateTime now = LocalDateTime.now(clock);

        EclassLink link = linkRepository.findByDeviceId(device.getId())
                .map(existing -> {
                    existing.relink(info.userid(), info.fullname(), encrypted, now);
                    return existing;
                })
                .orElseGet(() -> linkRepository.save(
                        new EclassLink(device, info.userid(), info.fullname(), encrypted, now)));

        // 첫 수집이 실패해도 연동 자체는 성공으로 둔다 — 다음 주기에 다시 시도한다
        try {
            syncService.syncLink(link);
        } catch (RuntimeException exception) {
            log.warn("initial eclass sync failed. linkId: {}", link.getId(), exception);
        }

        return EclassLinkResponse.from(link);
    }

    @Override
    @Transactional(readOnly = true)
    public EclassLinkResponse getStatus(String fid, String deviceToken) {
        return findLink(fid, deviceToken)
                .map(EclassLinkResponse::from)
                .orElseGet(EclassLinkResponse::unlinked);
    }

    @Override
    @Transactional
    public void unlink(String fid, String deviceToken) {
        findLink(fid, deviceToken).ifPresent(link -> {
            assignmentRepository.deleteAllByLinkId(link.getId());
            linkRepository.delete(link);
        });
    }

    @Override
    @Transactional
    public void syncNow(String fid, String deviceToken) {
        EclassLink link = findLink(fid, deviceToken)
                .orElseThrow(EclassLinkNotFoundException::new);

        LocalDateTime now = LocalDateTime.now(clock);
        if (link.isManualSyncOnCooldown(now, Duration.ofSeconds(manualCooldownSeconds))) {
            throw new EclassSyncCooldownException();
        }

        link.markManualSync(now);
        syncService.syncLink(link);
    }

    /**
     * 미등록 기기는 "연동 없음"과 같게 다룬다 — 이 표면은 어떤 상태에서도 에러 없이 떠야 한다.
     */
    private Optional<EclassLink> findLink(String fid, String deviceToken) {
        try {
            MemberDevice device = deviceResolver.resolve(fid, deviceToken);
            return linkRepository.findByDeviceId(device.getId());
        } catch (UnregisteredDeviceException exception) {
            return Optional.empty();
        }
    }
}
