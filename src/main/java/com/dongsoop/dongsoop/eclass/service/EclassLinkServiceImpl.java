package com.dongsoop.dongsoop.eclass.service;

import com.dongsoop.dongsoop.eclass.dto.EclassLinkResponse;
import com.dongsoop.dongsoop.eclass.dto.MoodleSiteInfoResponse;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.eclass.exception.EclassLinkNotFoundException;
import com.dongsoop.dongsoop.eclass.exception.EclassSyncCooldownException;
import com.dongsoop.dongsoop.eclass.repository.EclassAssignmentRepository;
import com.dongsoop.dongsoop.eclass.repository.EclassLinkRepository;
import com.dongsoop.dongsoop.eclass.util.EclassClient;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EclassLinkServiceImpl implements EclassLinkService {

    private final EclassDeviceAccessor deviceAccessor;
    private final EclassLinkRepository linkRepository;
    private final EclassAssignmentRepository assignmentRepository;
    private final EclassClient eclassClient;
    private final TextEncryptor eclassTokenEncryptor;
    private final EclassSyncService syncService;
    private final Clock clock;

    @Value("${eclass.sync.manual-cooldown-seconds}")
    private long manualCooldownSeconds;

    /**
     * 토큰 검증과 첫 수집은 이클래스 호출이라 느리다. 연동 저장만 트랜잭션으로 끊어 커밋한 뒤
     * 나머지를 트랜잭션 밖에서 수행해, 외부 응답을 기다리는 동안 DB 커넥션을 붙잡지 않는다.
     */
    @Override
    public EclassLinkResponse link(String fid, String deviceToken, String moodleToken) {
        // 기기 확인을 먼저 한다 — 자격 없는 요청으로 학교 서버를 호출하지 않는다
        MemberDevice device = deviceAccessor.resolveAccessible(fid, deviceToken)
                .orElseThrow(UnregisteredDeviceException::new);

        MoodleSiteInfoResponse info = eclassClient.getSiteInfo(moodleToken);
        EclassLink link = saveLink(device, moodleToken, info);

        // 첫 수집이 실패해도 연동 자체는 성공으로 둔다 — 다음 주기에 다시 시도한다
        try {
            syncService.syncLink(link);
        } catch (RuntimeException exception) {
            log.warn("initial eclass sync failed. linkId: {}", link.getId(), exception);
        }

        return EclassLinkResponse.from(link);
    }

    /**
     * 변경 감지에 기대지 않고 명시적으로 저장한다. 트랜잭션을 열어두면 뒤따르는 첫 수집이
     * 끝날 때까지 DB 커넥션을 붙잡게 되고, 자기 호출이라 트랜잭션 애너테이션도 걸리지 않는다.
     */
    private EclassLink saveLink(MemberDevice device, String moodleToken, MoodleSiteInfoResponse info) {
        String encrypted = eclassTokenEncryptor.encrypt(moodleToken);

        EclassLink link = linkRepository.findByDeviceId(device.getId())
                .map(existing -> {
                    existing.relink(info.fullname(), encrypted);
                    return existing;
                })
                .orElseGet(() -> new EclassLink(device, info.fullname(), encrypted));

        return linkRepository.save(link);
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
    public void syncNow(String fid, String deviceToken) {
        EclassLink link = findLink(fid, deviceToken)
                .orElseThrow(EclassLinkNotFoundException::new);

        // 만료된 연동은 토큰이 무효라 학교 서버를 부를 이유가 없다 — 앱은 GET /eclass/link로 상태를 본다
        if (!link.isActive()) {
            return;
        }

        markManualSync(link);
        syncService.syncLink(link);
    }

    private void markManualSync(EclassLink link) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (link.isManualSyncOnCooldown(now, Duration.ofSeconds(manualCooldownSeconds))) {
            throw new EclassSyncCooldownException();
        }

        link.markManualSync(now);
        linkRepository.save(link);
    }

    /**
     * 미등록 기기와 자격 없는 요청은 모두 "연동 없음"과 같게 다룬다 — 이 표면은 어떤 상태에서도 에러 없이 떠야 하고,
     * 남의 기기 식별자로 연동 정보를 읽거나 지울 수 있어서도 안 된다.
     */
    private Optional<EclassLink> findLink(String fid, String deviceToken) {
        return deviceAccessor.resolveAccessible(fid, deviceToken)
                .flatMap(device -> linkRepository.findByDeviceId(device.getId()));
    }
}
