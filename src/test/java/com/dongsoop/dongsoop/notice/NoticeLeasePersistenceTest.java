package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.*;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.department.entity.*;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notice.dto.HomeNotice;
import com.dongsoop.dongsoop.notice.entity.*;
import com.dongsoop.dongsoop.notice.repository.NoticeRepositoryCustomImpl;
import com.dongsoop.dongsoop.notice.reminder.entity.*;
import com.dongsoop.dongsoop.notice.reminder.repository.NoticeReminderRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(NoticeLeasePersistenceTest.QueryConfig.class)
class NoticeLeasePersistenceTest extends AbstractIntegrationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 15, 12, 0);
    @Autowired EntityManager em;
    @Autowired NoticeReminderRepository reminders;
    @Autowired NoticeRepositoryCustomImpl notices;
    @TestConfiguration
    static class QueryConfig {
        @Bean JPAQueryFactory queryFactory(EntityManager em) { return new JPAQueryFactory(em); }
    }

    @ParameterizedTest
    @ValueSource(ints = {-30, 30})
    void claimPersistsLeaseAndRejectsClaimUntilStrictlyExpired(int dueMinutes) {
        NoticeReminder value = reminder(1, NOW.plusMinutes(dueMinutes));
        Long id = value.getId(); Long deviceId = value.getDevice().getId(); flushAndClear();
        Duration grace = Duration.ofMinutes(10);
        LocalDateTime due = NOW.plusMinutes(dueMinutes);
        assertThat(reminders.claimForScheduling(id, NoticeReminderStatus.PENDING, NOW, grace)).contains(due);
        flushAndClear();
        LocalDateTime expected = NOW.plusMinutes(10);
        if (dueMinutes > 0) { expected = due.plusMinutes(10); }
        assertThat(reminders.findById(id).orElseThrow().getClaimedUntil()).isEqualTo(expected);
        assertThat(reminders.claimForScheduling(id, NoticeReminderStatus.PENDING, expected, grace)).isEmpty();
        assertThat(reminders.findByDeviceIdAndNoticeDetailsIdForUpdate(deviceId, 1L)).isPresent();
        assertThat(reminders.findByDeviceIdAndNoticeDetailsIdForUpdate(deviceId, -1L)).isEmpty();
        reminders.lockDevice(deviceId);
        assertThat(reminders.claimForScheduling(id, NoticeReminderStatus.PENDING, expected.plusNanos(1), grace)).contains(due);
        assertThat(reminders.claimForScheduling(-1L, NoticeReminderStatus.PENDING, NOW, grace)).isEmpty();
    }

    @Test
    void schedulerSelectionKeepsStatusTimeAndStrictLeaseBoundary() {
        NoticeReminder ready = reminder(1, NOW.plusMinutes(10));
        NoticeReminder expired = reminder(2, NOW.plusMinutes(20)); expired.claimUntil(NOW.minusSeconds(1));
        NoticeReminder held = reminder(3, NOW.plusMinutes(30)); held.claimUntil(NOW);
        NoticeReminder processing = reminder(4, NOW.plusMinutes(40)); processing.markProcessing(NOW.plusMinutes(10));
        reminder(5, NOW.plusMinutes(100));
        Long readyId = ready.getId(); Long expiredId = expired.getId(); Long heldId = held.getId(); flushAndClear();
        assertThat(reminders.findSchedulableIds(NoticeReminderStatus.PENDING, NOW, NOW.plusMinutes(65)))
                .containsExactly(readyId, expiredId);
        assertThat(reminders.findUpcomingIds(NoticeReminderStatus.PENDING, NOW.plusMinutes(65)))
                .containsExactly(readyId, expiredId, heldId);
    }

    @Test
    void recoveryOnlyResetsExpiredProcessingAndRetainsActiveLease() {
        NoticeReminder stale = reminder(1, NOW); stale.markProcessing(NOW.minusSeconds(1));
        NoticeReminder equal = reminder(2, NOW); equal.markProcessing(NOW);
        NoticeReminder future = reminder(3, NOW); future.markProcessing(NOW.plusMinutes(1));
        Long staleId = stale.getId(); Long equalId = equal.getId(); Long futureId = future.getId(); flushAndClear();
        assertThat(reminders.recoverStaleProcessing(NoticeReminderStatus.PROCESSING, NoticeReminderStatus.PENDING, NOW)).isEqualTo(1);
        flushAndClear();
        assertThat(reminders.findById(staleId).orElseThrow()).satisfies(row -> {
            assertThat(row.getStatus()).isEqualTo(NoticeReminderStatus.PENDING);
            assertThat(row.getClaimedUntil()).isNull();
        });
        assertThat(reminders.findById(equalId).orElseThrow().getStatus()).isEqualTo(NoticeReminderStatus.PROCESSING);
        assertThat(reminders.findById(futureId).orElseThrow().getStatus()).isEqualTo(NoticeReminderStatus.PROCESSING);
    }

    @Test
    void thirdSchedulingFailurePersistsFailedStateAndFurtherReleaseIsNoop() {
        NoticeReminder value = reminder(1, NOW); value.claimUntil(NOW.plusMinutes(10));
        Long id = value.getId(); flushAndClear();
        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(reminders.releaseAfterFailure(id, 3, NoticeReminderStatus.PENDING, NoticeReminderStatus.FAILED)).isEqualTo(1);
            flushAndClear();
            NoticeReminder loaded = reminders.findById(id).orElseThrow();
            assertThat(loaded.getRetryCount()).isEqualTo(attempt);
            assertThat(loaded.getClaimedUntil()).isNull();
        }
        assertThat(reminders.findById(id).orElseThrow().getStatus()).isEqualTo(NoticeReminderStatus.FAILED);
        assertThat(reminders.releaseAfterFailure(id, 3, NoticeReminderStatus.PENDING, NoticeReminderStatus.FAILED)).isZero();
        assertThat(reminders.releaseAfterFailure(-1L, 3, NoticeReminderStatus.PENDING, NoticeReminderStatus.FAILED)).isZero();
        assertThat(reminders.claimForScheduling(id, NoticeReminderStatus.PENDING, NOW, Duration.ofMinutes(10))).isEmpty();
    }

    @Test
    void homeQueriesApplySubscriptionsDeletedFilterOrderAndThreeRowLimit() {
        Department official = department(DepartmentType.DEPT_1001);
        Department own = department(DepartmentType.DEPT_2001);
        Department other = department(DepartmentType.DEPT_3001);
        notice(101, official); notice(102, own); notice(103, other);
        Notice deleted = notice(104, own); deleted.markDeleted(NOW);
        notice(105, official); notice(106, own); flushAndClear();
        assertThat(notices.searchHomeNotices()).extracting(HomeNotice::id).containsExactly(105L, 101L);
        assertThat(notices.searchHomeNotices(DepartmentType.DEPT_2001)).extracting(HomeNotice::id).containsExactly(106L, 105L, 102L);
        assertThat(notices.searchHomeNotices(List.of(DepartmentType.DEPT_3001))).extracting(HomeNotice::id).containsExactly(105L, 103L, 101L);
        assertThat(notices.findRecentNoticesIncludingDeleted(em.getReference(Department.class, DepartmentType.DEPT_2001), 2))
                .extracting(row -> row.getNoticeDetails().getId()).containsExactly(106L, 104L);
        assertThat(notices.searchHomeNotices().get(0)).satisfies(row -> {
            assertThat(row.title()).isEqualTo("notice-105");
            assertThat(row.link()).isEqualTo("/notice/105");
            assertThat(row.createdAt()).isEqualTo(NOW.toLocalDate());
        });
    }

    private NoticeReminder reminder(long number, LocalDateTime due) {
        MemberDevice device = MemberDevice.builder().deviceToken("token-" + number)
                .memberDeviceType(com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType.IOS).build(); em.persist(device);
        NoticeDetails details = details(number); NoticeReminder value = new NoticeReminder(device, details, due); em.persist(value); return value;
    }
    private Department department(DepartmentType type) { Department value = new Department(type, type.name(), "/notice"); em.persist(value); return value; }
    private NoticeDetails details(long id) { NoticeDetails value = new NoticeDetails(id, "school", "notice-" + id, "/notice/" + id, NOW.toLocalDate()); em.persist(value); return value; }
    private Notice notice(long id, Department department) { Notice value = new Notice(department, details(id)); em.persist(value); return value; }
    private void flushAndClear() { em.flush(); em.clear(); }
}
