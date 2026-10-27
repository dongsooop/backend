package com.dongsoop.dongsoop.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationList;
import com.dongsoop.dongsoop.notification.dto.NotificationOverview;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.exception.NotificationNotFoundException;
import com.dongsoop.dongsoop.notification.repository.NotificationDetailsRepository;
import com.dongsoop.dongsoop.notification.repository.NotificationRepository;
import com.dongsoop.dongsoop.notification.service.NotificationBadgeService;
import com.dongsoop.dongsoop.notification.service.NotificationSaveServiceImpl;
import com.dongsoop.dongsoop.notification.service.NotificationServiceImpl;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(NotificationPersistenceIntegrationTest.QueryConfig.class)
class NotificationPersistenceIntegrationTest extends AbstractIntegrationTest {
    @Autowired NotificationRepository notifications;
    @Autowired NotificationDetailsRepository details;
    @Autowired EntityManager entityManager;
    private NotificationSaveServiceImpl save;
    private NotificationServiceImpl service;
    private NotificationBadgeService badges;

    @TestConfiguration
    static class QueryConfig {
        @Bean
        JPAQueryFactory queryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
    }

    @BeforeEach
    void prepareServices() {
        badges = mock(NotificationBadgeService.class);
        save = new NotificationSaveServiceImpl(details, notifications);
        service = new NotificationServiceImpl(notifications, badges);
    }

    @Test
    void batchPersistsSharedDetailsAndSeparateMemberInboxes() {
        Member first = member("first");
        Member second = member("second");
        List<MemberNotification> saved = save.saveAll(List.of(first, second), "공지", "본문", NotificationType.NOTICE, "42");
        Long detailsId = saved.get(0).getDetails().getId();
        Long firstId = first.getId();
        Long secondId = second.getId();
        flushAndClear();
        assertThat(details.count()).isEqualTo(1);
        assertThat(notifications.count()).isEqualTo(2);
        assertThat(notifications.findUnreadCountByMemberId(firstId)).isEqualTo(1);
        assertThat(notifications.findUnreadCountByMemberId(secondId)).isEqualTo(1);
        List<NotificationList> inbox = notifications.getMemberNotifications(firstId, PageRequest.of(0, 10));
        assertThat(inbox).hasSize(1);
        NotificationList row = inbox.get(0);
        assertThat(row.id()).isEqualTo(detailsId);
        assertThat(row.title()).isEqualTo("공지");
        assertThat(row.body()).isEqualTo("본문");
        assertThat(row.type()).isEqualTo(NotificationType.NOTICE);
        assertThat(row.value()).isEqualTo("42");
        assertThat(row.isRead()).isFalse();
    }

    @Test
    void readingAndDeletingPersistsAndDoesNotAffectOtherMember() {
        Member first = member("first");
        Member second = member("second");
        List<MemberNotification> saved = save.saveAll(List.of(first, second), "공지", "본문", NotificationType.NOTICE, "42");
        Long detailsId = saved.get(0).getDetails().getId();
        Long firstId = first.getId();
        Long secondId = second.getId();
        flushAndClear();
        service.read(detailsId, firstId);
        flushAndClear();
        assertThat(notifications.findUnreadCountByMemberId(firstId)).isZero();
        assertThat(notifications.findUnreadCountByMemberId(secondId)).isEqualTo(1);
        verify(badges).pushBadge(firstId);
        service.deleteMemberNotification(detailsId, firstId);
        flushAndClear();
        assertThat(notifications.getMemberNotifications(firstId, PageRequest.of(0, 10))).isEmpty();
        assertThat(notifications.getMemberNotifications(secondId, PageRequest.of(0, 10))).hasSize(1);
    }

    @Test
    void anotherMemberCannotReadOrDeleteNotification() {
        Member owner = member("owner");
        Member other = member("other");
        MemberNotification saved = save.save(owner, "공지", "본문", NotificationType.NOTICE, "42");
        Long detailsId = saved.getDetails().getId();
        Long otherId = other.getId();
        Long ownerId = owner.getId();
        flushAndClear();
        assertThatThrownBy(() -> service.read(detailsId, otherId)).isInstanceOf(NotificationNotFoundException.class);
        assertThatThrownBy(() -> service.deleteMemberNotification(detailsId, otherId))
                .isInstanceOf(NotificationNotFoundException.class);
        assertThat(notifications.findUnreadCountByMemberId(ownerId)).isEqualTo(1);
        verifyNoInteractions(badges);
    }

    @Test
    void readAllUpdatesOnlyRequestedMemberAndPageKeepsPayload() {
        Member first = member("first");
        Member second = member("second");
        save.saveAll(List.of(first, second), "첫 공지", "본문1", NotificationType.NOTICE, "41");
        save.save(first, "둘째 공지", "본문2", NotificationType.NOTICE, "42");
        Long firstId = first.getId();
        Long secondId = second.getId();
        flushAndClear();
        service.readAll(firstId);
        flushAndClear();
        assertThat(notifications.findUnreadCountByMemberId(firstId)).isZero();
        assertThat(notifications.findUnreadCountByMemberId(secondId)).isEqualTo(1);
        when(badges.resolveByMemberId(firstId)).thenReturn(0);
        NotificationOverview overview = service.getNotifications(PageRequest.of(0, 1, Sort.by("id").descending()), firstId);
        assertThat(overview.unreadCount()).isZero();
        assertThat(overview.notificationLists()).extracting(NotificationList::title).containsExactly("둘째 공지");
        assertThat(overview.notificationLists().get(0).isRead()).isTrue();
    }

    private Member member(String name) {
        Member member = Member.builder().email(name + "@example.com").nickname(name).password("encoded").build();
        entityManager.persist(member);
        return member;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
