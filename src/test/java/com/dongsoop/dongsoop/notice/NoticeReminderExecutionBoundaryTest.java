package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminder;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminderStatus;
import com.dongsoop.dongsoop.notice.reminder.repository.NoticeReminderRepository;
import com.dongsoop.dongsoop.notice.reminder.service.NoticeReminderExecutionService;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class NoticeReminderExecutionBoundaryTest {
  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
  @Mock NoticeReminderRepository reminders;
  @Mock NotificationSendService notifications;
  @Mock TransactionTemplate transactions;
  @InjectMocks NoticeReminderExecutionService service;

  @BeforeEach
  void configurePrepareTransaction() {
    ReflectionTestUtils.setField(service, "universityDomain", "https://example.com");
    when(transactions.execute(any()))
        .thenAnswer(
            call -> {
              TransactionCallback<?> callback = call.getArgument(0);
              return callback.doInTransaction(null);
            });
  }

  @Test
  void missingReminderDoesNotSend() {
    service.execute(9L, LocalDateTime.now(ZONE));
    verifyNoInteractions(notifications);
  }

  @ParameterizedTest
  @EnumSource(
      value = NoticeReminderStatus.class,
      names = {"PROCESSING", "SENT", "FAILED"})
  void completedOrClaimedReminderIsNotSentAgain(NoticeReminderStatus status) {
    LocalDateTime dueAt = LocalDateTime.now(ZONE).minusDays(1);
    NoticeReminder reminder = reminder(dueAt);
    switch (status) {
      case PROCESSING -> reminder.markProcessing(dueAt.plusMinutes(10));
      case SENT -> reminder.markSent(dueAt);
      case FAILED -> reminder.markFailed();
      default -> throw new IllegalArgumentException("Unsupported fixture status");
    }
    when(reminders.findByIdForUpdate(9L)).thenReturn(Optional.of(reminder));
    service.execute(9L, dueAt);
    verifyNoInteractions(notifications);
    assertThat(reminder.getStatus()).isEqualTo(status);
  }

  @Test
  void rescheduledCallbackDoesNotSendOldReminder() {
    LocalDateTime original = LocalDateTime.now(ZONE).minusDays(1);
    NoticeReminder reminder = reminder(original.plusHours(1));
    when(reminders.findByIdForUpdate(9L)).thenReturn(Optional.of(reminder));
    service.execute(9L, original);
    assertThat(reminder.getStatus()).isEqualTo(NoticeReminderStatus.PENDING);
    verifyNoInteractions(notifications);
  }

  @Test
  void futureReminderDoesNotSendEarly() {
    LocalDateTime future = LocalDateTime.now(ZONE).plusDays(1);
    NoticeReminder reminder = reminder(future);
    when(reminders.findByIdForUpdate(9L)).thenReturn(Optional.of(reminder));
    service.execute(9L, future);
    assertThat(reminder.getClaimedUntil()).isNull();
    verifyNoInteractions(notifications);
  }

  @Test
  void dueReminderSendsExactDeviceAndNoticeAndMarksSent() {
    LocalDateTime dueAt = LocalDateTime.now(ZONE).minusDays(1);
    NoticeReminder reminder = reminder(dueAt);
    when(reminders.findByIdForUpdate(9L)).thenReturn(Optional.of(reminder));
    configureCompletionTransaction();
    service.execute(9L, dueAt);
    verify(notifications)
        .send(
            List.of("device-7"),
            new NotificationSend(
                9L, "공지 리마인더", "공지 제목", NotificationType.NOTICE, "https://example.com/notice/42"));
    assertThat(reminder.getStatus()).isEqualTo(NoticeReminderStatus.SENT);
    assertThat(reminder.getSentAt()).isNotNull();
    assertThat(reminder.getClaimedUntil()).isNull();
    assertThat(reminder.getRetryCount()).isZero();
  }

  @Test
  void thirdDeliveryFailureExhaustsRetryAndFurtherExecutionSkips() {
    LocalDateTime dueAt = LocalDateTime.now(ZONE).minusDays(1);
    NoticeReminder reminder = reminder(dueAt);
    when(reminders.findByIdForUpdate(9L)).thenReturn(Optional.of(reminder));
    configureCompletionTransaction();
    doThrow(new IllegalStateException("unavailable"))
        .when(notifications)
        .send(ArgumentMatchers.<List<String>>any(), any(NotificationSend.class));
    for (int attempt = 1; attempt <= 3; attempt++) {
      assertThatThrownBy(() -> service.execute(9L, dueAt))
          .isInstanceOf(IllegalStateException.class);
      assertThat(reminder.getRetryCount()).isEqualTo(attempt);
      assertThat(reminder.getClaimedUntil()).isNull();
    }
    assertThat(reminder.getStatus()).isEqualTo(NoticeReminderStatus.FAILED);
    service.execute(9L, dueAt);
    verify(notifications, times(3))
        .send(ArgumentMatchers.<List<String>>any(), any(NotificationSend.class));
  }

  private void configureCompletionTransaction() {
    doAnswer(
            call -> {
              Consumer<TransactionStatus> action = call.getArgument(0);
              action.accept(null);
              return null;
            })
        .when(transactions)
        .executeWithoutResult(any());
  }

  private NoticeReminder reminder(LocalDateTime dueAt) {
    NoticeDetails details =
        new NoticeDetails(42L, "학교", "공지 제목", "/notice/42", LocalDate.of(2026, 10, 14));
    return new NoticeReminder(
        MemberDevice.builder().id(7L).deviceToken("device-7").build(), details, dueAt);
  }
}
