package com.dongsoop.dongsoop.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import com.dongsoop.dongsoop.calendar.notification.query.CalendarNotificationQuery;
import com.dongsoop.dongsoop.calendar.notification.query.JpaCalendarNotificationQuery;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
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
@Import({CalendarNotificationQueryIntegrationTest.Config.class, JpaCalendarNotificationQuery.class})
class CalendarNotificationQueryIntegrationTest extends AbstractIntegrationTest {
    @Autowired EntityManager em;
    @Autowired CalendarNotificationQuery query;

    @TestConfiguration
    static class Config {
        @Bean JPAQueryFactory queryFactory(EntityManager em) { return new JPAQueryFactory(em); }
    }

    @Test
    void recipientsKeepMembersWithoutPersonalSchedulesAndDeduplicateDevices() {
        Member first = member("first");
        Member officialOnly = member("second");
        Member noDevice = member("third");
        device(first, "a"); device(first, "b"); device(officialOnly, "c");
        var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        em.persist(MemberSchedule.builder().member(first).title("Today").location("Room")
                .startAt(today.atTime(9, 0)).endAt(today.atTime(10, 0)).build());
        em.persist(MemberSchedule.builder().member(first).title("Tomorrow").location("Room")
                .startAt(today.plusDays(1).atStartOfDay()).endAt(today.plusDays(1).atTime(1, 0)).build());
        em.persist(MemberSchedule.builder().member(noDevice).title("No device").location("Room")
                .startAt(today.atTime(9, 0)).endAt(today.atTime(10, 0)).build());
        Long firstId = first.getId(); Long officialOnlyId = officialOnly.getId();
        em.flush(); em.clear();

        assertThat(query.findTodaySchedules()).hasSize(2).anySatisfy(row -> {
            assertThat(row.member().getId()).isEqualTo(firstId);
            assertThat(row.title()).isEqualTo("Today");
        }).anySatisfy(row -> {
            assertThat(row.member().getId()).isEqualTo(officialOnlyId);
            assertThat(row.title()).isNull();
        });
    }

    @Test
    void memberWithoutDevicesIsNotAnAlertRecipient() {
        member("first");
        em.flush(); em.clear();
        assertThat(query.findTodaySchedules()).isEmpty();
    }

    private Member member(String nickname) {
        var member = Member.builder().email(nickname + "@example.test").nickname(nickname).password("encoded").build();
        em.persist(member);
        return member;
    }
    private void device(Member member, String token) {
        em.persist(MemberDevice.builder().member(member).deviceToken(token).memberDeviceType(MemberDeviceType.IOS).build());
    }
}
