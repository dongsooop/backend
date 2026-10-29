package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.reminder.entity.*;
import com.dongsoop.dongsoop.notice.reminder.repository.NoticeReminderRepository;
import com.dongsoop.dongsoop.notice.reminder.scheduler.NoticeReminderScheduler;
import com.dongsoop.dongsoop.notice.reminder.service.NoticeReminderExecutionService;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulingContractTest {
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  @Mock NoticeReminderRepository reminders;
  @Mock NoticeReminderExecutionService execution;
  @Mock TaskScheduler tasks;
  @InjectMocks NoticeReminderScheduler scheduler;

  @Test
  void upcomingReminderIsClaimedWithGraceAndCallbackUsesOriginalDueTime() {
    LocalDateTime due = LocalDateTime.now(KST).plusMinutes(30);
    when(reminders.findById(9L)).thenReturn(Optional.of(reminder(due)));
    when(reminders.claimForScheduling(
            eq(9L), eq(NoticeReminderStatus.PENDING), any(), eq(Duration.ofMinutes(10))))
        .thenReturn(Optional.of(due));
    scheduler.scheduleIfUpcoming(9L);
    ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
    verify(tasks).schedule(callback.capture(), eq(due.atZone(KST).toInstant()));
    callback.getValue().run();
    verify(execution).execute(9L, due);
  }

  @Test
  void missingFarFutureOrUnclaimedReminderCannotRegisterTask() {
    scheduler.scheduleIfUpcoming(-1L);
    when(reminders.findById(9L))
        .thenReturn(Optional.of(reminder(LocalDateTime.now(KST).plusMinutes(70))));
    scheduler.scheduleIfUpcoming(9L);
    verify(reminders, never()).claimForScheduling(anyLong(), any(), any(), any());
    when(reminders.findById(10L))
        .thenReturn(Optional.of(reminder(LocalDateTime.now(KST).plusMinutes(1))));
    scheduler.scheduleIfUpcoming(10L);
    verifyNoInteractions(tasks, execution);
  }

  @ParameterizedTest
  @EnumSource(value = NoticeReminderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING")
  void nonPendingReminderIsNotScheduled(NoticeReminderStatus status) {
    NoticeReminder reminder = reminder(LocalDateTime.now(KST));
    ReflectionTestUtils.setField(reminder, "status", status);
    when(reminders.findById(9L)).thenReturn(Optional.of(reminder));
    scheduler.scheduleIfUpcoming(9L);
    verifyNoInteractions(tasks);
    verify(reminders, never()).claimForScheduling(anyLong(), any(), any(), any());
  }

  @Test
  void periodicScanRecoversStaleLeaseAndUsesSixtyFiveMinuteWindow() {
    LocalDateTime before = LocalDateTime.now(KST);
    scheduler.scheduleUpcoming();
    ArgumentCaptor<LocalDateTime> now = ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<LocalDateTime> until = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(reminders)
        .recoverStaleProcessing(
            eq(NoticeReminderStatus.PROCESSING), eq(NoticeReminderStatus.PENDING), now.capture());
    verify(reminders)
        .findSchedulableIds(eq(NoticeReminderStatus.PENDING), eq(now.getValue()), until.capture());
    assertThat(now.getValue()).isBetween(before, LocalDateTime.now(KST));
    assertThat(until.getValue()).isEqualTo(now.getValue().plusMinutes(65));
    verifyNoInteractions(tasks);
  }

  @Test
  void rejectedScheduleReleasesClaimAndPropagatesFailure() {
    LocalDateTime due = LocalDateTime.now(KST).plusMinutes(1);
    when(reminders.findSchedulableIds(any(), any(), any())).thenReturn(List.of(9L));
    when(reminders.claimForScheduling(eq(9L), any(), any(), any())).thenReturn(Optional.of(due));
    when(tasks.schedule(any(Runnable.class), any(Instant.class)))
        .thenThrow(new IllegalStateException("rejected"));
    assertThatThrownBy(scheduler::scheduleUpcoming).isInstanceOf(IllegalStateException.class);
    verify(reminders)
        .releaseAfterFailure(9L, 3, NoticeReminderStatus.PENDING, NoticeReminderStatus.FAILED);
    verifyNoInteractions(execution);
  }

  @Test
  void startupSchedulesOverduePendingAtNowAndExecutionFailureIsIsolated() {
    LocalDateTime due = LocalDateTime.now(KST).minusDays(1);
    NoticeReminder ready = reminder(due);
    NoticeReminder failed = reminder(due);
    failed.markFailed();
    when(reminders.findUpcomingIds(any(), any())).thenReturn(List.of(9L, 10L, 11L));
    when(reminders.findById(9L)).thenReturn(Optional.of(ready));
    when(reminders.findById(10L)).thenReturn(Optional.empty());
    when(reminders.findById(11L)).thenReturn(Optional.of(failed));
    Instant before = Instant.now();
    scheduler.scheduleOnStartup();
    ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
    ArgumentCaptor<Instant> time = ArgumentCaptor.forClass(Instant.class);
    verify(tasks).schedule(callback.capture(), time.capture());
    assertThat(time.getValue()).isBetween(before, Instant.now());
    doThrow(new IllegalStateException("delivery failed")).when(execution).execute(9L, due);
    assertThatCode(callback.getValue()::run).doesNotThrowAnyException();
    verify(reminders, never()).releaseAfterFailure(anyLong(), anyInt(), any(), any());
  }

  private NoticeReminder reminder(LocalDateTime time) {
    NoticeReminder value =
        new NoticeReminder(
            MemberDevice.builder().id(7L).build(),
            new NoticeDetails(42L, "school", "title", "/notice", LocalDate.of(2026, 10, 15)),
            time);
    ReflectionTestUtils.setField(value, "id", 9L);
    return value;
  }
}
