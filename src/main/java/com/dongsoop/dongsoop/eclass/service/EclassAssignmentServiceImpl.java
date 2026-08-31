package com.dongsoop.dongsoop.eclass.service;

import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentListResponse;
import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentResponse;
import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.eclass.entity.EclassLinkStatus;
import com.dongsoop.dongsoop.eclass.repository.EclassAssignmentRepository;
import com.dongsoop.dongsoop.eclass.repository.EclassLinkRepository;
import com.dongsoop.dongsoop.home.dto.HomeEclassSummary;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.service.NoticePreferenceDeviceResolver;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EclassAssignmentServiceImpl implements EclassAssignmentService {

    private static final int LIST_LIMIT = 100;
    private static final int NEAREST_LIMIT = 1;

    private final NoticePreferenceDeviceResolver deviceResolver;
    private final EclassLinkRepository linkRepository;
    private final EclassAssignmentRepository assignmentRepository;
    private final Clock clock;

    @Value("${eclass.base-url}")
    private String baseUrl;

    @Override
    @Transactional(readOnly = true)
    public EclassAssignmentListResponse getUpcoming(String fid, String deviceToken) {
        Optional<Long> deviceId = resolveDeviceId(fid, deviceToken);
        if (deviceId.isEmpty()) {
            return EclassAssignmentListResponse.unlinked();
        }

        Optional<EclassLink> link = linkRepository.findByDeviceId(deviceId.get());
        if (link.isEmpty()) {
            return EclassAssignmentListResponse.unlinked();
        }
        if (!link.get().isActive()) {
            return EclassAssignmentListResponse.expired();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<EclassAssignmentResponse> assignments =
                assignmentRepository.searchUpcomingByDevice(deviceId.get(), now, LIST_LIMIT).stream()
                        .map(assignment -> EclassAssignmentResponse.from(assignment, now.toLocalDate(), baseUrl))
                        .toList();

        return EclassAssignmentListResponse.of(assignments);
    }

    /**
     * 회원 홈: 요청 기기에 연동이 있으면 그 기기 기준으로, 없으면 그 회원이 가진 다른 기기의 연동까지 본다.
     */
    @Override
    @Transactional(readOnly = true)
    public HomeEclassSummary getHomeSummary(Long memberId, String fid, String deviceToken) {
        LocalDateTime now = LocalDateTime.now(clock);

        Optional<Long> deviceId = resolveDeviceId(fid, deviceToken);
        Optional<EclassLink> deviceLink = deviceId.flatMap(linkRepository::findByDeviceId);
        if (deviceLink.isPresent()) {
            if (!deviceLink.get().isActive()) {
                return HomeEclassSummary.expired();
            }

            return summarize(assignmentRepository.countUpcomingByDevice(deviceId.get(), now),
                    assignmentRepository.searchUpcomingByDevice(deviceId.get(), now, NEAREST_LIMIT), now);
        }

        // 요청 기기에 연동이 없으면 같은 회원의 다른 기기에서 연동한 과제를 보여준다
        if (linkRepository.findAllByMemberIdAndStatus(memberId, EclassLinkStatus.ACTIVE).isEmpty()) {
            return HomeEclassSummary.unlinked();
        }

        return summarize(assignmentRepository.countUpcomingByMember(memberId, now),
                assignmentRepository.searchUpcomingByMember(memberId, now, NEAREST_LIMIT), now);
    }

    @Override
    @Transactional(readOnly = true)
    public HomeEclassSummary getHomeSummary(String fid, String deviceToken) {
        Optional<Long> deviceId = resolveDeviceId(fid, deviceToken);
        Optional<EclassLink> link = deviceId.flatMap(linkRepository::findByDeviceId);
        if (link.isEmpty()) {
            return HomeEclassSummary.unlinked();
        }
        if (!link.get().isActive()) {
            return HomeEclassSummary.expired();
        }

        LocalDateTime now = LocalDateTime.now(clock);

        return summarize(assignmentRepository.countUpcomingByDevice(deviceId.get(), now),
                assignmentRepository.searchUpcomingByDevice(deviceId.get(), now, NEAREST_LIMIT), now);
    }

    private HomeEclassSummary summarize(long count, List<EclassAssignment> nearest, LocalDateTime now) {
        EclassAssignment first = nearest.isEmpty() ? null : nearest.get(0);

        return HomeEclassSummary.of(count, first, now.toLocalDate());
    }

    private Optional<Long> resolveDeviceId(String fid, String deviceToken) {
        try {
            MemberDevice device = deviceResolver.resolve(fid, deviceToken);
            return Optional.of(device.getId());
        } catch (UnregisteredDeviceException exception) {
            return Optional.empty();
        }
    }
}
