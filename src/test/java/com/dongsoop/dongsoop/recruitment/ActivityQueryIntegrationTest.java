package com.dongsoop.dongsoop.recruitment;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.common.config.JpaAuditingConfig;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.marketplace.entity.*;
import com.dongsoop.dongsoop.marketplace.query.dto.MarketplaceSummary;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepositoryCustom;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepositoryCustomImpl;
import com.dongsoop.dongsoop.recruitment.apply.project.entity.ProjectApply;
import com.dongsoop.dongsoop.recruitment.apply.study.entity.StudyApply;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.entity.TutoringApply;
import com.dongsoop.dongsoop.recruitment.board.dto.HomeRecruitment;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoardDepartment;
import com.dongsoop.dongsoop.recruitment.board.study.entity.StudyBoard;
import com.dongsoop.dongsoop.recruitment.board.study.entity.StudyBoardDepartment;
import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.TutoringBoard;
import com.dongsoop.dongsoop.recruitment.query.RecruitmentQuery;
import com.dongsoop.dongsoop.recruitment.query.dto.RecruitmentStatusType;
import com.dongsoop.dongsoop.recruitment.query.dto.RecruitmentSummary;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ActivityQueryIntegrationTest.QueryConfig.class, MarketplaceBoardRepositoryCustomImpl.class,
        JpaAuditingConfig.class})
class ActivityQueryIntegrationTest extends AbstractIntegrationTest {
    @Autowired EntityManager em;
    @Autowired RecruitmentQuery recruitment;
    @Autowired MarketplaceBoardRepositoryCustom marketplace;
    private Member author;
    private Member applicant;
    private Department department;
    private LocalDateTime now;

    @TestConfiguration
    static class QueryConfig {
        @Bean JPAQueryFactory queryFactory(EntityManager em) { return new JPAQueryFactory(em); }
    }

    @BeforeEach
    void fixtures() {
        now = (LocalDateTime) em.createNativeQuery("SELECT LOCALTIMESTAMP", LocalDateTime.class).getSingleResult();
        department = new Department(DepartmentType.DEPT_2001, "학과", null);
        em.persist(department);
        author = member("author");
        applicant = member("apply");
    }

    @Test
    void recruitmentUnionPreservesGlobalOrderingPaginationApplicantCountsAndOwnerScope() {
        ProjectBoard project = project("project", 3);
        StudyBoard study = study("study", 2);
        TutoringBoard tutoring = tutoring("tutoring", 1);
        em.persist(ProjectApply.builder().projectBoard(project).member(applicant).introduction("소개")
                .motivation("동기").applyTime(now.minusHours(1)).build());
        em.persist(ProjectApply.builder().projectBoard(project).member(member("other")).introduction("소개")
                .motivation("동기").applyTime(now.minusHours(2)).build());
        em.persist(StudyApply.builder().studyBoard(study).member(applicant).introduction("소개")
                .motivation("동기").applyTime(now.minusHours(3)).build());
        em.persist(TutoringApply.builder().tutoringBoard(tutoring).member(applicant).introduction("소개")
                .motivation("동기").applyTime(now.minusHours(2)).build());
        project("deleted", 0).setDeleted(true);
        fixApplicationTimes(project, study, tutoring);
        flushAndClear();

        var opened = recruitment.findOpenedRecruitmentsByMemberId(author.getId(), PageRequest.of(0, 10));
        assertThat(opened).extracting(RecruitmentSummary::getTitle).containsExactly("tutoring", "study", "project");
        assertThat(opened).extracting(RecruitmentSummary::getBoardType)
                .containsExactly(RecruitmentType.TUTORING, RecruitmentType.STUDY, RecruitmentType.PROJECT);
        assertThat(opened).extracting(RecruitmentSummary::getStatus).containsOnly(RecruitmentStatusType.RECRUITING);
        assertThat(opened).extracting(RecruitmentSummary::getVolunteer).containsExactly(1L, 1L, 2L);
        assertThat(opened).extracting(RecruitmentSummary::getDepartmentTypeList).containsOnly("학과");
        assertThat(recruitment.findOpenedRecruitmentsByMemberId(author.getId(), PageRequest.of(1, 1)))
                .extracting(RecruitmentSummary::getTitle).containsExactly("study");
        assertThat(recruitment.findOpenedRecruitmentsByMemberId(applicant.getId(), PageRequest.of(0, 10))).isEmpty();
        assertThat(recruitment.findApplyRecruitmentsByMemberId(applicant.getId(), PageRequest.of(0, 10)))
                .extracting(RecruitmentSummary::getTitle).containsExactly("project", "tutoring", "study");
        assertThat(recruitment.findApplyRecruitmentsByMemberId(applicant.getId(), PageRequest.of(1, 1)))
                .extracting(RecruitmentSummary::getTitle).containsExactly("tutoring");
        assertThat(recruitment.findApplyRecruitmentsByMemberId(author.getId(), PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    void homeUnionLimitsAcrossTypesAndFiltersDepartmentActiveWindowAndDeletedRows() {
        project("old", 4);
        project("project", 3);
        study("study", 2);
        tutoring("tutoring", 1);
        project("deleted", 0).setDeleted(true);
        ProjectBoard waiting = ProjectBoard.builder().title("waiting").content("내용").tags("tag")
                .author(author).startAt(now.plusDays(1)).endAt(now.plusDays(2)).createdAt(now).build();
        em.persist(waiting);
        em.persist(new ProjectBoardDepartment(waiting, department));
        StudyBoard ended = StudyBoard.builder().title("ended").content("내용").tags("tag")
                .author(author).startAt(now.minusDays(2)).endAt(now.minusDays(1)).createdAt(now).build();
        em.persist(ended);
        em.persist(new StudyBoardDepartment(ended, department));
        flushAndClear();

        assertThat(recruitment.searchHomeRecruitment()).extracting(HomeRecruitment::getTitle)
                .containsExactly("tutoring", "study", "project");
        assertThat(recruitment.searchHomeRecruitment(DepartmentType.DEPT_2001.name()))
                .extracting(HomeRecruitment::getTitle).containsExactly("tutoring", "study", "project");
        assertThat(recruitment.searchHomeRecruitment(DepartmentType.DEPT_3001.name())).isEmpty();
        assertThat(recruitment.findOpenedRecruitmentsByMemberId(author.getId(), PageRequest.of(0, 10)))
                .filteredOn(row -> row.getTitle().equals("waiting"))
                .extracting(RecruitmentSummary::getStatus).containsExactly(RecruitmentStatusType.WAITING);
        assertThat(recruitment.findOpenedRecruitmentsByMemberId(author.getId(), PageRequest.of(0, 10)))
                .filteredOn(row -> row.getTitle().equals("ended"))
                .extracting(RecruitmentSummary::getStatus).containsExactly(RecruitmentStatusType.ENDED);
    }

    @Test
    void marketplaceSummaryKeepsFirstImageDistinctContactsAuthorAndPagination() {
        MarketplaceBoard first = market("first", 2, author);
        MarketplaceBoard second = market("second", 1, author);
        market("other", 0, applicant);
        MarketplaceBoard deleted = market("deleted", 0, author);
        deleted.setDeleted(true);
        em.persist(MarketplaceImage.builder().marketplaceBoard(second).url("first-image")
                .createdAt(now.minusHours(2)).build());
        em.persist(MarketplaceImage.builder().marketplaceBoard(second).url("later-image")
                .createdAt(now.minusHours(1)).build());
        em.persist(MarketplaceContact.builder().marketplaceId(second.getId()).applicant(applicant).build());
        em.persist(MarketplaceContact.builder().marketplaceId(second.getId()).applicant(member("other")).build());
        flushAndClear();

        var rows = marketplace.findOpenedMarketplaceByAuthorIdAndPage(author.getId(), PageRequest.of(0, 10));
        assertThat(rows).extracting(MarketplaceSummary::title).containsExactly("second", "first");
        assertThat(rows.get(0).contactCount()).isEqualTo(2L);
        assertThat(rows.get(0).imageUrl()).isEqualTo("first-image");
        assertThat(rows.get(0).price()).isEqualTo(1000L);
        assertThat(rows.get(0).type()).isEqualTo(MarketplaceType.SELL);
        assertThat(rows.get(0).status()).isEqualTo(MarketplaceBoardStatus.OPEN);
        assertThat(rows.get(1).contactCount()).isZero();
        assertThat(rows.get(1).imageUrl()).isNull();
        assertThat(marketplace.findOpenedMarketplaceByAuthorIdAndPage(author.getId(), PageRequest.of(1, 1)))
                .extracting(MarketplaceSummary::id).containsExactly(first.getId());
    }

    private void fixApplicationTimes(ProjectBoard project, StudyBoard study, TutoringBoard tutoring) {
        // Auditing can replace @CreatedDate values when the full suite has initialized its listener.
        em.flush();
        em.createNativeQuery("UPDATE project_apply SET apply_time = :at WHERE project_board_id = :id AND member_id = :member")
                .setParameter("at", now.minusHours(1)).setParameter("id", project.getId())
                .setParameter("member", applicant.getId()).executeUpdate();
        em.createNativeQuery("UPDATE study_apply SET apply_time = :at WHERE study_board_id = :id AND member_id = :member")
                .setParameter("at", now.minusHours(3)).setParameter("id", study.getId())
                .setParameter("member", applicant.getId()).executeUpdate();
        em.createNativeQuery("UPDATE tutoring_apply SET apply_time = :at WHERE tutoring_board_id = :id AND member_id = :member")
                .setParameter("at", now.minusHours(2)).setParameter("id", tutoring.getId())
                .setParameter("member", applicant.getId()).executeUpdate();
    }

    private ProjectBoard project(String title, int age) {
        ProjectBoard board = ProjectBoard.builder().title(title).content("내용").tags("tag")
                .startAt(now.minusDays(1)).endAt(now.plusDays(1)).author(author)
                .createdAt(now.minusHours(age)).build();
        em.persist(board);
        em.persist(new ProjectBoardDepartment(board, department));
        return board;
    }

    private StudyBoard study(String title, int age) {
        StudyBoard board = StudyBoard.builder().title(title).content("내용").tags("tag")
                .startAt(now.minusDays(1)).endAt(now.plusDays(1)).author(author)
                .createdAt(now.minusHours(age)).build();
        em.persist(board);
        em.persist(new StudyBoardDepartment(board, department));
        return board;
    }

    private TutoringBoard tutoring(String title, int age) {
        TutoringBoard board = TutoringBoard.builder().title(title).content("내용").tags("tag")
                .startAt(now.minusDays(1)).endAt(now.plusDays(1)).author(author).department(department)
                .createdAt(now.minusHours(age)).build();
        em.persist(board);
        return board;
    }

    private MarketplaceBoard market(String title, int age, Member owner) {
        MarketplaceBoard board = MarketplaceBoard.builder().title(title).content("내용").price(1000L)
                .author(owner).type(MarketplaceType.SELL).createdAt(now.minusHours(age)).build();
        em.persist(board);
        return board;
    }

    private Member member(String name) {
        Member member = Member.builder().email(name + "@example.com").nickname(name).password("encoded").build();
        em.persist(member);
        return member;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
