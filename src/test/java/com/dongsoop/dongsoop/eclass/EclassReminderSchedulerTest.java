package com.dongsoop.dongsoop.eclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.eclass.notification.EclassNotification;
import com.dongsoop.dongsoop.eclass.repository.EclassAssignmentRepository;
import com.dongsoop.dongsoop.eclass.scheduler.EclassReminderScheduler;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EclassReminderSchedulerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 1, 8, 0);

    @Mock
    private EclassAssignmentRepository assignmentRepository;
    @Mock
    private EclassNotification eclassNotification;

    private EclassReminderScheduler scheduler;
    private EclassLink link;

    @BeforeEach
    void setUp() {
        when(eclassNotification.sendReminder(any(), anyInt())).thenReturn(true);
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.ofHours(9)), ZoneId.of("Asia/Seoul"));
        scheduler = new EclassReminderScheduler(assignmentRepository, eclassNotification, clock);
        ReflectionTestUtils.setField(scheduler, "daysBefore", List.of(3, 1, 0));

        MemberDevice device = MemberDevice.builder()
                .id(1L)
                .deviceToken("fcm-token")
                .build();
        link = new EclassLink(device, "백승민", "encrypted");
    }

    private EclassAssignment assignment(long assignId, LocalDateTime dueAt) {
        return new EclassAssignment(link, assignId, 9000L + assignId, "자료구조", "과제 " + assignId, dueAt, dueAt);
    }

    @Test
    @DisplayName("D-3, D-1, 당일 과제에 각각 알림을 보낸다")
    void sendsForConfiguredDays() {
        EclassAssignment threeDays = assignment(1L, NOW.plusDays(3).withHour(23).withMinute(55));
        EclassAssignment oneDay = assignment(2L, NOW.plusDays(1).withHour(23).withMinute(55));
        EclassAssignment today = assignment(3L, NOW.withHour(23).withMinute(55));
        when(assignmentRepository.searchReminderTargets(any(), any()))
                .thenReturn(List.of(threeDays, oneDay, today));

        scheduler.remind();

        verify(eclassNotification).sendReminder(threeDays, 3);
        verify(eclassNotification).sendReminder(oneDay, 1);
        verify(eclassNotification).sendReminder(today, 0);
    }

    @Test
    @DisplayName("설정에 없는 남은 일수는 보내지 않는다")
    void skipsUnconfiguredDays() {
        EclassAssignment twoDays = assignment(1L, NOW.plusDays(2).withHour(23).withMinute(55));
        when(assignmentRepository.searchReminderTargets(any(), any())).thenReturn(List.of(twoDays));

        scheduler.remind();

        verify(eclassNotification, never()).sendReminder(any(), anyInt());
    }

    @Test
    @DisplayName("같은 단계는 두 번 보내지 않는다")
    void doesNotRepeatSameStage() {
        EclassAssignment reminded = assignment(1L, NOW.plusDays(3).withHour(23).withMinute(55));
        reminded.markReminded(3);
        when(assignmentRepository.searchReminderTargets(any(), any())).thenReturn(List.of(reminded));

        scheduler.remind();

        verify(eclassNotification, never()).sendReminder(any(), anyInt());
    }

    @Test
    @DisplayName("D-3을 보낸 과제도 D-1이 되면 다시 알린다")
    void sendsNextStage() {
        EclassAssignment reminded = assignment(1L, NOW.plusDays(1).withHour(23).withMinute(55));
        reminded.markReminded(3);
        when(assignmentRepository.searchReminderTargets(any(), any())).thenReturn(List.of(reminded));

        scheduler.remind();

        verify(eclassNotification).sendReminder(reminded, 1);
        assertThat(reminded.getLastRemindedDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("발송 후 남은 단계를 저장한다")
    void savesRemindedStage() {
        EclassAssignment target = assignment(1L, NOW.plusDays(1).withHour(23).withMinute(55));
        List<EclassAssignment> targets = List.of(target);
        when(assignmentRepository.searchReminderTargets(any(), any())).thenReturn(targets);

        scheduler.remind();

        verify(assignmentRepository).saveAll(eq(targets));
        assertThat(target.getLastRemindedDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("조회 시작 시각은 현재 시각이라 이미 지난 마감은 대상에서 빠진다")
    void queriesFromNow() {
        when(assignmentRepository.searchReminderTargets(any(), any())).thenReturn(List.of());

        scheduler.remind();

        verify(assignmentRepository).searchReminderTargets(eq(NOW),
                eq(NOW.toLocalDate().plusDays(3).atTime(java.time.LocalTime.MAX)));
    }

    @Test
    @DisplayName("알림이 꺼져 건너뛴 과제는 보낸 것으로 기록하지 않는다")
    void doesNotMarkSkippedReminder() {
        EclassAssignment target = assignment(1L, NOW.plusDays(1).withHour(23).withMinute(55));
        when(assignmentRepository.searchReminderTargets(any(), any())).thenReturn(List.of(target));
        when(eclassNotification.sendReminder(target, 1)).thenReturn(false);

        scheduler.remind();

        assertThat(target.getLastRemindedDays()).isNull();
        verify(assignmentRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("발송이 실패해도 나머지 과제는 계속 보낸다")
    void continuesAfterSendFailure() {
        EclassAssignment failing = assignment(1L, NOW.plusDays(1).withHour(23).withMinute(55));
        EclassAssignment following = assignment(2L, NOW.plusDays(3).withHour(23).withMinute(55));
        when(assignmentRepository.searchReminderTargets(any(), any())).thenReturn(List.of(failing, following));
        when(eclassNotification.sendReminder(failing, 1)).thenThrow(new IllegalStateException("fcm down"));
        when(eclassNotification.sendReminder(following, 3)).thenReturn(true);

        scheduler.remind();

        assertThat(failing.getLastRemindedDays()).isNull();
        assertThat(following.getLastRemindedDays()).isEqualTo(3);
    }
}
