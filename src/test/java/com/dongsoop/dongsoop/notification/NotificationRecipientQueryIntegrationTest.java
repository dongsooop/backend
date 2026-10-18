package com.dongsoop.dongsoop.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.dto.DeviceSubscription;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.notice.notification.query.JpaNoticeNotificationTargetQuery;
import com.dongsoop.dongsoop.notice.notification.query.NoticeNotificationTargetQuery;
import com.dongsoop.dongsoop.notice.preference.entity.DeviceNoticePreference;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.recipient.query.JpaNotificationRecipientQuery;
import com.dongsoop.dongsoop.notification.recipient.query.NotificationRecipientQuery;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
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
@Import({NotificationRecipientQueryIntegrationTest.QueryConfig.class,
        JpaNotificationRecipientQuery.class, JpaNoticeNotificationTargetQuery.class})
class NotificationRecipientQueryIntegrationTest extends AbstractIntegrationTest {
    @Autowired EntityManager em;
    @Autowired NotificationRecipientQuery recipients;
    @Autowired NoticeNotificationTargetQuery notices;

    @TestConfiguration
    static class QueryConfig {
        @Bean JPAQueryFactory queryFactory(EntityManager em) { return new JPAQueryFactory(em); }
    }

    @Test
    void defaultEnabledRecipientsExcludeDisabledWebTokenlessAndOtherMembers() {
        Member owner = member("owner");
        device(owner, "enabled", MemberDeviceType.ANDROID);
        MemberDevice disabled = device(owner, "disabled", MemberDeviceType.IOS);
        em.persist(new NotificationSetting(disabled, NotificationType.CHAT, false));
        device(owner, "web", MemberDeviceType.WEB);
        device(owner, null, MemberDeviceType.IOS);
        device(member("other"), "other", MemberDeviceType.IOS);
        device(null, "guest", MemberDeviceType.IOS);
        flushAndClear();

        assertThat(recipients.findDevicesWithNotification(
                new MemberDeviceFindCondition(List.of(owner.getId()), NotificationType.CHAT)))
                .extracting(MemberDeviceDto::deviceToken).containsExactly("enabled");
    }

    @Test
    void defaultDisabledRecipientsRequireExplicitOptIn() {
        Member owner = member("owner");
        device(owner, "unset", MemberDeviceType.ANDROID);
        MemberDevice enabled = device(owner, "opt-in", MemberDeviceType.IOS);
        em.persist(new NotificationSetting(enabled, NotificationType.MARKETING, true));
        MemberDevice disabled = device(owner, "opt-out", MemberDeviceType.IOS);
        em.persist(new NotificationSetting(disabled, NotificationType.MARKETING, false));
        flushAndClear();

        assertThat(recipients.findDevicesWithNotification(
                new MemberDeviceFindCondition(List.of(owner.getId()), NotificationType.MARKETING)))
                .extracting(MemberDeviceDto::deviceToken).containsExactly("opt-in");
    }

    @Test
    void noticeTargetsUseSubscriptionsAndUniversityNoticeIncludesUnsubscribedGuests() {
        Department department = new Department(DepartmentType.DEPT_2001, "학과", null);
        em.persist(department);
        MemberDevice subscribed = device(member("owner"), "subscribed", MemberDeviceType.ANDROID);
        em.persist(new DeviceNoticePreference(subscribed, department));
        MemberDevice guest = device(null, "guest", MemberDeviceType.IOS);
        MemberDevice disabled = device(null, "disabled", MemberDeviceType.IOS);
        em.persist(new DeviceNoticePreference(disabled, department));
        em.persist(new NotificationSetting(disabled, NotificationType.NOTICE, false));
        MemberDevice web = device(null, "web", MemberDeviceType.WEB);
        em.persist(new DeviceNoticePreference(web, department));
        MemberDevice tokenless = device(null, null, MemberDeviceType.ANDROID);
        em.persist(new DeviceNoticePreference(tokenless, department));
        flushAndClear();

        assertThat(notices.searchDevicesByDepartments(List.of(DepartmentType.DEPT_2001)))
                .extracting(MemberDevice::getId).containsExactly(subscribed.getId());
        assertThat(notices.searchDevicesByDepartments(List.of(DepartmentType.DEPT_1001)))
                .extracting(MemberDevice::getId).containsExactlyInAnyOrder(subscribed.getId(), guest.getId());
        assertThat(notices.findSubscriptionsByDeviceIds(List.of(subscribed.getId(), guest.getId())))
                .containsExactly(new DeviceSubscription(subscribed.getId(), DepartmentType.DEPT_2001));
        assertThat(notices.findSubscriptionsByDeviceIds(List.of())).isEmpty();
    }

    private Member member(String name) {
        Member member = Member.builder().email(name + "@dongyang.ac.kr")
                .nickname(name).password("encoded").build();
        em.persist(member);
        return member;
    }

    private MemberDevice device(Member owner, String token, MemberDeviceType type) {
        MemberDevice device = MemberDevice.builder().member(owner).deviceToken(token)
                .memberDeviceType(type).build();
        em.persist(device);
        return device;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
