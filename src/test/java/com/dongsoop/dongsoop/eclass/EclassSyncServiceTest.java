package com.dongsoop.dongsoop.eclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.common.crypto.AesGcmEncryptor;
import com.dongsoop.dongsoop.eclass.client.EclassClient;
import com.dongsoop.dongsoop.eclass.client.dto.MoodleAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.eclass.entity.EclassLinkStatus;
import com.dongsoop.dongsoop.eclass.exception.EclassApiException;
import com.dongsoop.dongsoop.eclass.exception.EclassInvalidTokenException;
import com.dongsoop.dongsoop.eclass.notification.EclassNotification;
import com.dongsoop.dongsoop.eclass.repository.EclassAssignmentRepository;
import com.dongsoop.dongsoop.eclass.repository.EclassLinkRepository;
import com.dongsoop.dongsoop.eclass.service.EclassSyncService.SyncOutcome;
import com.dongsoop.dongsoop.eclass.service.EclassSyncServiceImpl;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EclassSyncServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 1, 10, 0);

    @Mock
    private EclassLinkRepository linkRepository;
    @Mock
    private EclassAssignmentRepository assignmentRepository;
    @Mock
    private EclassClient eclassClient;
    @Mock
    private AesGcmEncryptor encryptor;
    @Mock
    private EclassNotification eclassNotification;

    private EclassSyncServiceImpl syncService;
    private EclassLink link;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.ofHours(9)), ZONE);
        syncService = new EclassSyncServiceImpl(linkRepository, assignmentRepository, eclassClient, encryptor,
                eclassNotification, clock);
        ReflectionTestUtils.setField(syncService, "windowPastDays", 1);
        ReflectionTestUtils.setField(syncService, "windowFutureDays", 30);
        ReflectionTestUtils.setField(syncService, "submissionCheckDays", 3);
        ReflectionTestUtils.setField(syncService, "threadCount", 2);
        ReflectionTestUtils.setField(syncService, "requestDelayMs", 0L);
        ReflectionTestUtils.setField(syncService, "abortFailureRatio", 0.5);
        ReflectionTestUtils.setField(syncService, "relinkTimeoutHours", 24L);

        MemberDevice device = MemberDevice.builder()
                .id(1L)
                .deviceToken("fcm-token")
                .build();
        link = new EclassLink(device, 14077L, "백승민", "encrypted", NOW.minusDays(1));
        ReflectionTestUtils.setField(link, "id", 10L);

        when(encryptor.decrypt("encrypted")).thenReturn("moodle-token");
        when(assignmentRepository.findAllByLinkId(10L)).thenReturn(List.of());
    }

    private MoodleAssignment moodleAssignment(long assignId, LocalDateTime dueAt) {
        long epochSecond = dueAt.toInstant(ZoneOffset.ofHours(9)).getEpochSecond();
        return new MoodleAssignment(assignId, 9000L + assignId, "자료구조", "과제 " + assignId, epochSecond, epochSecond);
    }

    @SuppressWarnings("unchecked")
    private List<EclassAssignment> capturedSaved() {
        ArgumentCaptor<List<EclassAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(assignmentRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("수집 창 밖의 과제와 마감 없는 과제는 저장하지 않는다")
    void filtersOutOfWindow() {
        MoodleAssignment noDueDate = new MoodleAssignment(500L, 9500L, "자바", "마감 없는 과제", 0L, 0L);
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(
                noDueDate,
                moodleAssignment(501L, NOW.minusDays(5)),
                moodleAssignment(502L, NOW.plusDays(40)),
                moodleAssignment(503L, NOW.plusDays(3))));
        when(eclassClient.isSubmitted(anyString(), anyLong())).thenReturn(false);

        SyncOutcome outcome = syncService.syncLink(link);

        assertThat(outcome).isEqualTo(SyncOutcome.SYNCED);
        assertThat(capturedSaved()).extracting(EclassAssignment::getAssignId).containsExactly(503L);
    }

    @Test
    @DisplayName("제출한 과제는 submitted로 저장된다")
    void marksSubmitted() {
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(moodleAssignment(601L, NOW.plusDays(2))));
        when(eclassClient.isSubmitted("moodle-token", 601L)).thenReturn(true);

        syncService.syncLink(link);

        assertThat(capturedSaved().get(0).isSubmitted()).isTrue();
    }

    @Test
    @DisplayName("이미 제출한 과제는 제출 상태를 다시 묻지 않는다")
    void skipsSubmissionCheckForSubmitted() {
        EclassAssignment submitted = new EclassAssignment(link, 601L, 9601L, "자료구조", "과제 601",
                NOW.plusDays(2), NOW.plusDays(2));
        submitted.markSubmitted(NOW.minusDays(1));
        when(assignmentRepository.findAllByLinkId(10L)).thenReturn(List.of(submitted));
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(moodleAssignment(601L, NOW.plusDays(2))));

        syncService.syncLink(link);

        verify(eclassClient, never()).isSubmitted(anyString(), anyLong());
    }

    @Test
    @DisplayName("응답에서 사라진 과제는 삭제로 표시한다")
    void marksRemoved() {
        EclassAssignment existing = new EclassAssignment(link, 601L, 9601L, "자료구조", "과제 601",
                NOW.plusDays(2), NOW.plusDays(2));
        when(assignmentRepository.findAllByLinkId(10L)).thenReturn(List.of(existing));
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of());

        syncService.syncLink(link);

        assertThat(capturedSaved()).singleElement()
                .extracting(EclassAssignment::isRemoved)
                .isEqualTo(true);
    }

    @Test
    @DisplayName("마감이 리마인드 창 밖이면 제출 여부를 묻지 않는다")
    void skipsSubmissionCheckOutsideReminderWindow() {
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(
                moodleAssignment(601L, NOW.plusDays(2)),
                moodleAssignment(602L, NOW.plusDays(10))));
        when(eclassClient.isSubmitted(anyString(), anyLong())).thenReturn(false);

        syncService.syncLink(link);

        verify(eclassClient).isSubmitted("moodle-token", 601L);
        verify(eclassClient, never()).isSubmitted("moodle-token", 602L);
    }

    @Test
    @DisplayName("실패가 임계치를 넘으면 남은 연동은 건드리지 않고 주기를 접는다")
    void abortsWhenFailureRatioExceeded() {
        List<EclassLink> links = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            MemberDevice device = MemberDevice.builder().id((long) i).deviceToken("fcm-" + i).build();
            EclassLink each = new EclassLink(device, 1L, "백승민", "encrypted", NOW.minusDays(1));
            ReflectionTestUtils.setField(each, "id", (long) i);
            links.add(each);
        }
        ReflectionTestUtils.setField(syncService, "threadCount", 1);
        when(linkRepository.findAllByStatus(EclassLinkStatus.ACTIVE)).thenReturn(links);
        when(linkRepository.findAllByStatus(EclassLinkStatus.EXPIRED)).thenReturn(List.of());
        when(assignmentRepository.findAllByLinkId(anyLong())).thenReturn(List.of());
        when(eclassClient.getAssignments("moodle-token"))
                .thenThrow(new EclassApiException("mod_assign", "server"));

        syncService.syncAll();

        // 20건 전부가 아니라 임계치에 닿은 시점에서 멈춘다
        verify(eclassClient, atMost(10)).getAssignments("moodle-token");
    }

    @Test
    @DisplayName("마감이 바뀌면 리마인드 단계를 초기화한다")
    void resetsReminderStageOnDueDateChange() {
        EclassAssignment existing = new EclassAssignment(link, 601L, 9601L, "자료구조", "과제 601",
                NOW.plusDays(2), NOW.plusDays(2));
        existing.markReminded(1);
        when(assignmentRepository.findAllByLinkId(10L)).thenReturn(List.of(existing));
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(moodleAssignment(601L, NOW.plusDays(9))));
        when(eclassClient.isSubmitted(anyString(), anyLong())).thenReturn(false);

        syncService.syncLink(link);

        assertThat(capturedSaved().get(0).getLastRemindedDays()).isNull();
    }

    @Test
    @DisplayName("토큰이 무효하면 연동을 만료로 바꾸고 재발급 무음 푸시를 한 번만 보낸다")
    void expiresOnInvalidToken() {
        when(eclassClient.getAssignments("moodle-token")).thenThrow(new EclassInvalidTokenException());

        SyncOutcome first = syncService.syncLink(link);
        SyncOutcome second = syncService.syncLink(link);

        assertThat(first).isEqualTo(SyncOutcome.TOKEN_EXPIRED);
        assertThat(second).isEqualTo(SyncOutcome.TOKEN_EXPIRED);
        assertThat(link.getStatus()).isEqualTo(EclassLinkStatus.EXPIRED);
        verify(eclassNotification, times(1)).sendRelinkSilent(link);
        verify(assignmentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("이클래스 API 오류는 연동 상태를 바꾸지 않는다")
    void keepsLinkOnApiError() {
        when(eclassClient.getAssignments("moodle-token")).thenThrow(new EclassApiException("mod_assign", "server"));

        SyncOutcome outcome = syncService.syncLink(link);

        assertThat(outcome).isEqualTo(SyncOutcome.FAILED);
        assertThat(link.getStatus()).isEqualTo(EclassLinkStatus.ACTIVE);
        verify(assignmentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("재발급 지시 후 24시간이 지나면 보이는 만료 알림으로 승격한다")
    void promotesOverdueRelink() {
        link.expire(NOW.minusHours(25));
        when(linkRepository.findAllByStatus(EclassLinkStatus.ACTIVE)).thenReturn(List.of());
        when(linkRepository.findAllByStatus(EclassLinkStatus.EXPIRED)).thenReturn(List.of(link));

        syncService.syncAll();

        verify(eclassNotification).sendExpiredNotice(link);
        assertThat(link.getExpiredNotifiedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("재발급 지시 후 24시간이 지나지 않았으면 보이는 알림을 보내지 않는다")
    void doesNotPromoteBeforeTimeout() {
        link.expire(NOW.minusHours(23));
        when(linkRepository.findAllByStatus(EclassLinkStatus.ACTIVE)).thenReturn(List.of());
        when(linkRepository.findAllByStatus(EclassLinkStatus.EXPIRED)).thenReturn(List.of(link));

        syncService.syncAll();

        verify(eclassNotification, never()).sendExpiredNotice(any());
    }



    @Test
    @DisplayName("연동 성공 시 마지막 수집 시각을 남긴다")
    void marksSyncedAt() {
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of());

        syncService.syncLink(link);

        assertThat(link.getLastSyncedAt()).isEqualTo(NOW);
        verify(linkRepository).save(link);
    }

    @Test
    @DisplayName("Instant 변환은 서울 기준 시각을 쓴다")
    void convertsEpochInSeoul() {
        long epochSecond = Instant.parse("2026-09-03T14:55:00Z").getEpochSecond();
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(
                new MoodleAssignment(701L, 9701L, "자료구조", "과제", epochSecond, 0L)));
        when(eclassClient.isSubmitted(anyString(), eq(701L))).thenReturn(false);

        syncService.syncLink(link);

        assertThat(capturedSaved().get(0).getDueAt()).isEqualTo(LocalDateTime.of(2026, 9, 3, 23, 55));
    }

    @Test
    @DisplayName("마감이 앞당겨지면 변경 알림을 보낸다")
    void notifiesWhenDueDateAdvanced() {
        EclassAssignment existing = new EclassAssignment(link, 601L, 9601L, "자료구조", "과제 601",
                NOW.plusDays(9), NOW.plusDays(9));
        when(assignmentRepository.findAllByLinkId(10L)).thenReturn(List.of(existing));
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(moodleAssignment(601L, NOW.plusDays(2))));
        when(eclassClient.isSubmitted(anyString(), anyLong())).thenReturn(false);

        syncService.syncLink(link);

        verify(eclassNotification).sendDueDateChanged(link, existing);
        assertThat(existing.getDueAt()).isEqualTo(NOW.plusDays(2));
    }

    @Test
    @DisplayName("마감이 미뤄지면 변경 알림을 보내지 않는다 — 리마인드가 새 일정으로 다시 나간다")
    void doesNotNotifyWhenDueDatePostponed() {
        EclassAssignment existing = new EclassAssignment(link, 601L, 9601L, "자료구조", "과제 601",
                NOW.plusDays(2), NOW.plusDays(2));
        when(assignmentRepository.findAllByLinkId(10L)).thenReturn(List.of(existing));
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(moodleAssignment(601L, NOW.plusDays(9))));
        when(eclassClient.isSubmitted(anyString(), anyLong())).thenReturn(false);

        syncService.syncLink(link);

        verify(eclassNotification, never()).sendDueDateChanged(any(), any());
    }

    @Test
    @DisplayName("마감이 그대로면 변경 알림을 보내지 않는다")
    void doesNotNotifyWhenDueDateUnchanged() {
        EclassAssignment existing = new EclassAssignment(link, 601L, 9601L, "자료구조", "과제 601",
                NOW.plusDays(2), NOW.plusDays(2));
        when(assignmentRepository.findAllByLinkId(10L)).thenReturn(List.of(existing));
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(moodleAssignment(601L, NOW.plusDays(2))));
        when(eclassClient.isSubmitted(anyString(), anyLong())).thenReturn(false);

        syncService.syncLink(link);

        verify(eclassNotification, never()).sendDueDateChanged(any(), any());
    }

    @Test
    @DisplayName("이미 제출한 과제는 마감이 앞당겨져도 알리지 않는다")
    void doesNotNotifySubmittedAssignment() {
        EclassAssignment existing = new EclassAssignment(link, 601L, 9601L, "자료구조", "과제 601",
                NOW.plusDays(9), NOW.plusDays(9));
        existing.markSubmitted(NOW.minusDays(1));
        when(assignmentRepository.findAllByLinkId(10L)).thenReturn(List.of(existing));
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(moodleAssignment(601L, NOW.plusDays(2))));

        syncService.syncLink(link);

        verify(eclassNotification, never()).sendDueDateChanged(any(), any());
    }

    @Test
    @DisplayName("새로 들어온 과제는 변경 알림 대상이 아니다")
    void doesNotNotifyNewAssignment() {
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(moodleAssignment(601L, NOW.plusDays(2))));
        when(eclassClient.isSubmitted(anyString(), anyLong())).thenReturn(false);

        syncService.syncLink(link);

        verify(eclassNotification, never()).sendDueDateChanged(any(), any());
    }

    @Test
    @DisplayName("선제 재발급 뒤 실제로 만료되면 재발급 타이머를 다시 잰다")
    void resetsRelinkTimerOnExpire() {
        ReflectionTestUtils.setField(link, "relinkRequestedAt", NOW.minusHours(30));
        when(eclassClient.getAssignments("moodle-token")).thenThrow(new EclassInvalidTokenException());

        syncService.syncLink(link);

        assertThat(link.getRelinkRequestedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Moodle 상한을 넘는 과목명·과제명은 잘라서 저장한다")
    void shortensOverlongText() {
        String longText = "가".repeat(300);
        long epochSecond = NOW.plusDays(2).toInstant(ZoneOffset.ofHours(9)).getEpochSecond();
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(
                new MoodleAssignment(801L, 9801L, longText, longText, epochSecond, 0L)));
        when(eclassClient.isSubmitted(anyString(), anyLong())).thenReturn(false);

        syncService.syncLink(link);

        EclassAssignment saved = capturedSaved().get(0);
        assertThat(saved.getCourseName()).hasSize(255);
        assertThat(saved.getTitle()).hasSize(255);
    }

    @Test
    @DisplayName("제출 조회 중 토큰이 만료돼도 그때까지 모은 과제는 저장한다")
    void savesPartialResultWhenTokenExpiresMidway() {
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(moodleAssignment(901L, NOW.plusDays(2))));
        when(eclassClient.isSubmitted(anyString(), anyLong())).thenThrow(new EclassInvalidTokenException());

        SyncOutcome outcome = syncService.syncLink(link);

        assertThat(outcome).isEqualTo(SyncOutcome.TOKEN_EXPIRED);
        assertThat(capturedSaved()).extracting(EclassAssignment::getAssignId).containsExactly(901L);
    }

    @Test
    @DisplayName("같은 회차에 제출로 확인된 과제는 마감 변경을 알리지 않는다")
    void doesNotNotifyWhenSubmittedDuringSync() {
        EclassAssignment existing = new EclassAssignment(link, 601L, 9601L, "자료구조", "과제 601",
                NOW.plusDays(9), NOW.plusDays(9));
        when(assignmentRepository.findAllByLinkId(10L)).thenReturn(List.of(existing));
        when(eclassClient.getAssignments("moodle-token")).thenReturn(List.of(moodleAssignment(601L, NOW.plusDays(2))));
        when(eclassClient.isSubmitted(anyString(), anyLong())).thenReturn(true);

        syncService.syncLink(link);

        verify(eclassNotification, never()).sendDueDateChanged(any(), any());
    }
}
