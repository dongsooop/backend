package com.dongsoop.dongsoop.oauth;

import static org.assertj.core.api.Assertions.*;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.department.entity.*;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.dto.DeleteMember;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepositoryCustomImpl;
import com.dongsoop.dongsoop.memberdevice.entity.*;
import com.dongsoop.dongsoop.oauth.entity.*;
import com.dongsoop.dongsoop.oauth.repository.MemberSocialAccountRepositoryCustomImpl;
import com.dongsoop.dongsoop.role.entity.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({IdentityPersistenceContractTest.QueryConfig.class,
        com.dongsoop.dongsoop.notification.recipient.query.JpaNotificationRecipientQuery.class})
class IdentityPersistenceContractTest extends AbstractIntegrationTest {
    @Autowired EntityManager em;
    @Autowired com.dongsoop.dongsoop.notification.recipient.query.NotificationRecipientQuery recipients;
    @Autowired MemberRepositoryCustomImpl members;
    @Autowired MemberSocialAccountRepositoryCustomImpl accounts;

    @TestConfiguration
    static class QueryConfig {
        @Bean JPAQueryFactory queryFactory(EntityManager em) { return new JPAQueryFactory(em); }
    }

    @Test
    void socialIdentityProjectsMemberAndAllRolesAndScopesProviderLookups() {
        Member member = member("first");
        Member other = member("second");
        role(member, 1L, RoleType.USER); role(member, 2L, RoleType.ADMIN);
        MemberSocialAccount google = account(member, "google-7", OAuthProviderType.GOOGLE);
        account(member, "apple-7", OAuthProviderType.APPLE);
        account(other, "other-google", OAuthProviderType.GOOGLE);
        Long id = member.getId();
        var created = google.getCreatedAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        flushAndClear();
        var result = accounts.findMemberSocialAccountDTO("google-7", OAuthProviderType.GOOGLE).orElseThrow();
        assertThat(result.member().getId()).isEqualTo(id);
        assertThat(result.providerId()).isEqualTo("google-7");
        assertThat(result.providerType()).isEqualTo(OAuthProviderType.GOOGLE);
        assertThat(result.roleType()).containsExactlyInAnyOrder(RoleType.USER, RoleType.ADMIN);
        assertThat(accounts.findMemberSocialAccountDTO("google-7", OAuthProviderType.APPLE)).isEmpty();
        assertThat(accounts.findByMemberIdAndProviderType(id, OAuthProviderType.GOOGLE)).isPresent();
        assertThat(accounts.findByMemberIdAndProviderType(id, OAuthProviderType.KAKAO)).isEmpty();
        assertThat(accounts.findByMemberAndProviderType(em.getReference(Member.class, id), OAuthProviderType.APPLE)).isPresent();
        assertThat(accounts.findByMemberAndProviderType(em.getReference(Member.class, id), OAuthProviderType.KAKAO)).isEmpty();
        assertThat(accounts.findAllMemberSocialAccountOverview(id)).hasSize(2).anySatisfy(row -> {
            assertThat(row.providerType()).isEqualTo(OAuthProviderType.GOOGLE);
            assertThat(row.createdAt()).isCloseTo(created, org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MICROS));
        });
    }

    @Test
    void roleFreeAccountCanBeReadDirectlyAndMissingIdentityIsEmpty() {
        Member member = member("first");
        account(member, "apple-7", OAuthProviderType.APPLE);
        flushAndClear();
        assertThat(accounts.findByMemberIdAndProviderType(member.getId(), OAuthProviderType.APPLE)).isPresent();
        assertThat(accounts.findMemberSocialAccountDTO("missing", OAuthProviderType.APPLE)).isEmpty();
        assertThat(accounts.findAllMemberSocialAccountOverview(-1L)).isEmpty();
    }

    @Test
    void lockLoadsRequestedMemberAndMissingMemberHasDomainError() {
        Member first = member("first");
        Long id = first.getId(); flushAndClear();
        assertThat(members.findAndLockById(id).getId()).isEqualTo(id);
        assertThatThrownBy(() -> members.findAndLockById(-1L)).isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void softDeletePersistsAnonymizationAndIsIdempotent() {
        Member first = member("first");
        Long id = first.getId(); flushAndClear();
        assertThat(members.softDelete(new DeleteMember(id, "password-alias"))).isEqualTo(1);
        flushAndClear();
        Member loaded = em.find(Member.class, id);
        assertThat(loaded.getNickname()).isEqualTo("익명_" + id);
        assertThat(loaded.getPassword()).isEqualTo("password-alias");
        assertThat(loaded.getStudentId()).isNull();
        assertThat(loaded.isDeleted()).isTrue();
        assertThat(members.softDelete(new DeleteMember(id, "other-alias"))).isZero();
        assertThat(members.softDelete(new DeleteMember(-1L, "other-alias"))).isZero();
    }

    @Test
    void loginProjectionKeepsDepartmentAndRolesAndDeviceQueryDeduplicatesMembers() {
        Department department = new Department(DepartmentType.DEPT_2001, "소프트웨어", "/notice");
        em.persist(department);
        Member first = Member.builder().email("first@example.com").nickname("first").password("encoded").department(department).build();
        em.persist(first);
        Member noDevice = member("second");
        Role user = role(first, 1L, RoleType.USER);
        em.persist(new MemberRole(noDevice, user));
        em.persist(MemberDevice.builder().member(first).deviceToken("a").memberDeviceType(MemberDeviceType.IOS).build());
        em.persist(MemberDevice.builder().member(first).deviceToken("b").memberDeviceType(MemberDeviceType.ANDROID).build());
        Long id = first.getId(); flushAndClear();
        var login = members.findLoginMemberDetailById(id).orElseThrow();
        assertThat(login.getNickname()).isEqualTo("first");
        assertThat(login.getEmail()).isEqualTo("first@example.com");
        assertThat(login.getDepartmentType()).isEqualTo(DepartmentType.DEPT_2001);
        assertThat(login.getRole()).containsExactly(RoleType.USER);
        assertThat(recipients.findMembersByRoleWithDevice(RoleType.USER)).extracting(Member::getId).containsExactly(id);
        assertThat(members.findLoginMemberDetailById(-1L)).isEmpty();
        members.softDelete(new DeleteMember(id, "alias")); flushAndClear();
        assertThat(recipients.findMembersByRoleWithDevice(RoleType.USER)).isEmpty();
    }

    private Member member(String name) {
        Member member = Member.builder().email(name + "@example.com").nickname(name).password("encoded").studentId("20260001").build();
        em.persist(member); return member;
    }
    private Role role(Member member, Long id, RoleType type) {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "id", id); ReflectionTestUtils.setField(role, "roleType", type);
        em.persist(role); em.persist(new MemberRole(member, role)); return role;
    }
    private MemberSocialAccount account(Member member, String provider, OAuthProviderType type) {
        var account = new MemberSocialAccount(new MemberSocialAccountId(provider, type), member);
        em.persist(account); return account;
    }
    private void flushAndClear() { em.flush(); em.clear(); }
}
