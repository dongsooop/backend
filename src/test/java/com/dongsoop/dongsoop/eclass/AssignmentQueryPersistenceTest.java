package com.dongsoop.dongsoop.eclass;

import static org.assertj.core.api.Assertions.*;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.eclass.entity.*;
import com.dongsoop.dongsoop.eclass.repository.EclassAssignmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.entity.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
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
@Import(AssignmentQueryPersistenceTest.QueryConfig.class)
class AssignmentQueryPersistenceTest extends AbstractIntegrationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 15, 12, 0);
    @Autowired EntityManager em;
    @Autowired EclassAssignmentRepository queries;
    @TestConfiguration
    static class QueryConfig {
        @Bean JPAQueryFactory queryFactory(EntityManager em) { return new JPAQueryFactory(em); }
    }

    @Test
    void memberAndDeviceViewsScopeOwnershipActiveLinkAndDifferentListCountSemantics() {
        Member owner = member("first"); Member other = member("second");
        EclassLink first = link(owner, "first"); EclassLink second = link(owner, "second");
        EclassLink foreign = link(other, "other"); EclassLink guest = link(null, "guest");
        EclassLink expired = link(owner, "expired"); expired.expire(NOW);
        assignment(first, 1, "near", 1);
        EclassAssignment submitted = assignment(first, 2, "submitted", 2); submitted.updateSubmission(true, NOW);
        EclassAssignment removed = assignment(first, 3, "removed", 3); removed.markRemoved(NOW);
        assignment(first, 4, "equal", 0); assignment(first, 5, "past", -1); assignment(first, 6, "far", 70);
        assignment(second, 1, "second-device", 5); assignment(foreign, 1, "foreign", 1);
        assignment(guest, 1, "guest", 1); assignment(expired, 1, "expired", 1);
        Long ownerId = owner.getId(); Long deviceId = first.getDevice().getId(); Long guestId = guest.getDevice().getId();
        flushAndClear();
        assertThat(queries.searchUpcomingByMember(ownerId, NOW, 10)).extracting(EclassAssignment::getTitle)
                .containsExactly("near", "second-device", "far", "submitted");
        assertThat(queries.countUpcomingByMember(ownerId, NOW)).isEqualTo(3);
        assertThat(queries.searchUpcomingByDevice(deviceId, NOW, 2)).extracting(EclassAssignment::getTitle).containsExactly("near", "far");
        assertThat(queries.countUpcomingByDevice(deviceId, NOW)).isEqualTo(2);
        assertThat(queries.searchUpcomingByDevice(guestId, NOW, 10)).extracting(EclassAssignment::getTitle).containsExactly("guest");
        assertThat(queries.countUpcomingByDevice(guestId, NOW)).isEqualTo(1);
        assertThat(queries.searchUpcomingByMember(-1L, NOW, 10)).isEmpty();
        assertThat(queries.countUpcomingByMember(-1L, NOW)).isZero();
        assertThat(queries.countUpcomingByDevice(-1L, NOW)).isZero();
    }

    @Test
    void remindersUseExclusiveLowerInclusiveUpperBoundAndKeepGuestDevices() {
        EclassLink active = link(member("owner"), "active");
        EclassLink guest = link(null, "guest");
        EclassLink expired = link(null, "expired"); expired.expire(NOW);
        assignment(active, 1, "lower-equal", 0); assignment(active, 2, "upper-equal", 5);
        assignment(guest, 1, "guest", 1); assignment(active, 3, "after-upper", 6);
        EclassAssignment submitted = assignment(active, 4, "submitted", 2); submitted.updateSubmission(true, NOW);
        EclassAssignment removed = assignment(active, 5, "removed", 3); removed.markRemoved(NOW);
        assignment(expired, 1, "expired", 4); flushAndClear();
        var targets = queries.searchReminderTargets(NOW, NOW.plusMinutes(5));
        assertThat(targets).extracting(EclassAssignment::getTitle).containsExactly("guest", "upper-equal");
        assertThat(targets.get(0).getLink().getDevice().getMember()).isNull();
        assertThat(targets.get(1).getLink().getDevice().getMember().getNickname()).isEqualTo("owner");
        assertThat(queries.searchReminderTargets(NOW.plusDays(1), NOW.plusDays(2))).isEmpty();
    }

    private Member member(String name) { Member value = Member.builder().email(name + "@example.com").nickname(name).password("encoded").build(); em.persist(value); return value; }
    private EclassLink link(Member member, String token) {
        MemberDevice device = MemberDevice.builder().member(member).deviceToken(token).memberDeviceType(MemberDeviceType.IOS).build(); em.persist(device);
        EclassLink value = new EclassLink(device, "student", "fake-ciphertext"); em.persist(value); return value;
    }
    private EclassAssignment assignment(EclassLink link, long id, String title, int minutes) {
        EclassAssignment value = new EclassAssignment(link, id, id, "course", title, NOW.plusMinutes(minutes), null); em.persist(value); return value;
    }
    private void flushAndClear() { em.flush(); em.clear(); }
}
