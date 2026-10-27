package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.reminder.dto.NoticeReminderResponse;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminder;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminderStatus;
import com.dongsoop.dongsoop.notice.reminder.exception.InvalidNoticeReminderTimeException;
import com.dongsoop.dongsoop.notice.reminder.exception.NoticeReminderNotFoundException;
import com.dongsoop.dongsoop.notice.reminder.repository.NoticeReminderRepository;
import com.dongsoop.dongsoop.notice.reminder.scheduler.NoticeReminderScheduler;
import com.dongsoop.dongsoop.notice.reminder.service.NoticeReminderService;
import com.dongsoop.dongsoop.notice.repository.NoticeDetailsRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class NoticeReminderSchedulingServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    @Mock NoticeReminderRepository reminders;
    @Mock NoticeDetailsRepository notices;
    @Mock MemberDeviceRepository devices;
    @Mock NoticeReminderScheduler scheduler;
    @InjectMocks NoticeReminderService service;

    @BeforeEach
    void beginSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void upsertSavesPayloadAndSchedulesOnlyAfterCommit() {
        MemberDevice device = device();
        NoticeDetails notice = notice();
        LocalDateTime future = LocalDateTime.now(ZONE).plusDays(1);
        when(devices.findByFid("fid-7")).thenReturn(Optional.of(device));
        when(notices.findById(42L)).thenReturn(Optional.of(notice));
        when(reminders.save(any())).thenAnswer(call -> {
            NoticeReminder reminder = call.getArgument(0);
            ReflectionTestUtils.setField(reminder, "id", 9L);
            return reminder;
        });
        NoticeReminderResponse response = service.upsert(42L, "fid-7", "device-7", future);
        assertThat(response).isEqualTo(new NoticeReminderResponse(42L, future));
        ArgumentCaptor<NoticeReminder> saved = ArgumentCaptor.forClass(NoticeReminder.class);
        verify(reminders).save(saved.capture());
        assertThat(saved.getValue().getDevice()).isSameAs(device);
        assertThat(saved.getValue().getNoticeDetails()).isSameAs(notice);
        assertThat(saved.getValue().getRemindAt()).isEqualTo(future);
        assertThat(saved.getValue().getStatus()).isEqualTo(NoticeReminderStatus.PENDING);
        verify(devices).lockById(7L);
        verifyNoInteractions(scheduler);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(scheduler).scheduleIfUpcoming(9L);
    }

    @Test
    void existingSentReminderIsRescheduledAndRetryStateReset() {
        MemberDevice device = device();
        NoticeDetails notice = notice();
        LocalDateTime past = LocalDateTime.now(ZONE).minusDays(1);
        NoticeReminder existing = new NoticeReminder(device, notice, past);
        ReflectionTestUtils.setField(existing, "id", 9L);
        existing.markDeliveryFailed(3);
        existing.markSent(past);
        LocalDateTime future = LocalDateTime.now(ZONE).plusDays(1);
        when(devices.findByFid("fid-7")).thenReturn(Optional.of(device));
        when(notices.findById(42L)).thenReturn(Optional.of(notice));
        when(reminders.findByDeviceIdAndNoticeDetailsIdForUpdate(7L, 42L)).thenReturn(Optional.of(existing));
        when(reminders.save(existing)).thenReturn(existing);
        service.upsert(42L, "fid-7", "device-7", future);
        assertThat(existing.getRemindAt()).isEqualTo(future);
        assertThat(existing.getStatus()).isEqualTo(NoticeReminderStatus.PENDING);
        assertThat(existing.getRetryCount()).isZero();
        assertThat(existing.getSentAt()).isNull();
        assertThat(existing.getClaimedUntil()).isNull();
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(scheduler).scheduleIfUpcoming(9L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void absentFidOrTokenCannotAccessReminders(String absent) {
        assertThatThrownBy(() -> service.get(42L, absent, "device-7")).isInstanceOf(UnregisteredDeviceException.class);
        assertThatThrownBy(() -> service.delete(42L, "fid-7", absent)).isInstanceOf(UnregisteredDeviceException.class);
        verifyNoInteractions(reminders, notices, devices, scheduler);
    }

    @Test
    void mismatchedTokenCannotAccessAnotherDevicesReminder() {
        when(devices.findByFid("fid-7")).thenReturn(Optional.of(device()));
        assertThatThrownBy(() -> service.get(42L, "fid-7", "other-device")).isInstanceOf(UnregisteredDeviceException.class);
        verifyNoInteractions(reminders);
    }

    @Test
    void pastTimeIsRejectedBeforeAnyPersistence() {
        LocalDateTime past = LocalDateTime.now(ZONE).minusDays(1);
        assertThatThrownBy(() -> service.upsert(42L, "fid-7", "device-7", past))
                .isInstanceOf(InvalidNoticeReminderTimeException.class);
        verifyNoInteractions(reminders, notices, devices, scheduler);
    }

    @Test
    void ownDeviceCanReadAndDeleteItsReminder() {
        MemberDevice device = device();
        LocalDateTime future = LocalDateTime.now(ZONE).plusDays(1);
        NoticeReminder reminder = new NoticeReminder(device, notice(), future);
        when(devices.findByFid("fid-7")).thenReturn(Optional.of(device));
        when(reminders.findByDeviceIdAndNoticeDetailsId(7L, 42L)).thenReturn(Optional.of(reminder));
        assertThat(service.get(42L, "fid-7", "device-7")).isEqualTo(new NoticeReminderResponse(42L, future));
        service.delete(42L, "fid-7", "device-7");
        verify(reminders).delete(reminder);
    }

    @Test
    void missingOwnReminderHasDomainError() {
        when(devices.findByFid("fid-7")).thenReturn(Optional.of(device()));
        assertThatThrownBy(() -> service.get(42L, "fid-7", "device-7")).isInstanceOf(NoticeReminderNotFoundException.class);
        assertThatThrownBy(() -> service.delete(42L, "fid-7", "device-7")).isInstanceOf(NoticeReminderNotFoundException.class);
    }

    private MemberDevice device() {
        return MemberDevice.builder().id(7L).fid("fid-7").deviceToken("device-7").build();
    }

    private NoticeDetails notice() {
        return new NoticeDetails(42L, "학교", "공지 제목", "/notice/42", LocalDate.of(2026, 10, 14));
    }
}
