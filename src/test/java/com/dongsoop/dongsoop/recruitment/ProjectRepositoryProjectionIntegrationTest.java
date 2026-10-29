package com.dongsoop.dongsoop.recruitment;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.recruitment.apply.project.entity.ProjectApply;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoardDepartment;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepositoryCustomImpl;
import com.dongsoop.dongsoop.recruitment.board.projection.ProjectRecruitmentProjection;
import com.dongsoop.dongsoop.recruitment.board.projection.StudyRecruitmentProjection;
import com.dongsoop.dongsoop.recruitment.board.projection.TutoringRecruitmentProjection;
import com.dongsoop.dongsoop.recruitment.board.study.repository.StudyBoardRepositoryCustomImpl;
import com.dongsoop.dongsoop.recruitment.board.tutoring.repository.TutoringBoardRepositoryCustomImpl;
import com.dongsoop.dongsoop.recruitment.repository.RecruitmentRepositoryUtilsImpl;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
@Import({
  ProjectRepositoryProjectionIntegrationTest.QueryConfig.class,
  RecruitmentRepositoryUtilsImpl.class,
  ProjectRecruitmentProjection.class,
  StudyRecruitmentProjection.class,
  TutoringRecruitmentProjection.class,
  ProjectBoardRepositoryCustomImpl.class,
  StudyBoardRepositoryCustomImpl.class,
  TutoringBoardRepositoryCustomImpl.class
})
class ProjectRepositoryProjectionIntegrationTest extends AbstractIntegrationTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 12, 0);
  @Autowired ProjectBoardRepositoryCustom boards;
  @Autowired EntityManager em;
  private Department department;
  private Member author;

  @TestConfiguration
  static class QueryConfig {
    @Bean
    JPAQueryFactory queryFactory(EntityManager em) {
      return new JPAQueryFactory(em);
    }

    @Bean
    Clock clock() {
      ZoneId zone = ZoneId.of("Asia/Seoul");
      return Clock.fixed(NOW.atZone(zone).toInstant(), zone);
    }
  }

  @BeforeEach
  void fixtures() {
    department = new Department(DepartmentType.DEPT_2001, "컴퓨터소프트웨어공학과", "/notice");
    em.persist(department);
    author = member("author");
  }

  @Test
  void activeFilterAndPaginationExcludePastFutureAndDeletedBoards() {
    ProjectBoard first = board("첫 모집", NOW.minusDays(1), NOW.plusDays(1));
    ProjectBoard second = board("둘째 모집", NOW.minusDays(1), NOW.plusDays(1));
    board("지난 모집", NOW.minusDays(3), NOW.minusDays(1));
    board("예정 모집", NOW.plusDays(1), NOW.plusDays(3));
    ProjectBoard deleted = board("삭제 모집", NOW.minusDays(1), NOW.plusDays(1));
    deleted.setDeleted(true);
    board("시작 경계", NOW, NOW.plusDays(1));
    board("종료 경계", NOW.minusDays(1), NOW);
    Long firstId = first.getId();
    Long secondId = second.getId();
    flushAndClear();
    assertThat(boards.findProjectBoardOverviewsByPage(PageRequest.of(0, 100, Sort.by("id"))))
        .extracting(RecruitmentOverview::id)
        .containsExactly(firstId, secondId);
    PageRequest firstPage = PageRequest.of(0, 1, Sort.by("id"));
    PageRequest secondPage = PageRequest.of(1, 1, Sort.by("id"));
    assertThat(boards.findProjectBoardOverviewsByPage(firstPage))
        .extracting(RecruitmentOverview::id)
        .containsExactly(firstId);
    assertThat(boards.findProjectBoardOverviewsByPage(secondPage))
        .extracting(RecruitmentOverview::id)
        .containsExactly(secondId);
    assertThat(
            boards.findProjectBoardOverviewsByPageAndDepartmentType(
                DepartmentType.DEPT_3001, firstPage))
        .isEmpty();
  }

  @Test
  void projectionKeepsApplicantCountDepartmentAndViewState() {
    ProjectBoard board = board("모집", NOW.minusDays(1), NOW.plusDays(1));
    Member first = member("first");
    Member second = member("second");
    em.persist(
        ProjectApply.builder()
            .projectBoard(board)
            .member(first)
            .introduction("소개1")
            .motivation("동기1")
            .applyTime(NOW.minusHours(1))
            .build());
    em.persist(
        ProjectApply.builder()
            .projectBoard(board)
            .member(second)
            .introduction("소개2")
            .motivation("동기2")
            .applyTime(NOW.minusHours(1))
            .build());
    Long boardId = board.getId();
    Long authorId = author.getId();
    flushAndClear();
    RecruitmentOverview overview =
        boards
            .findProjectBoardOverviewsByPageAndDepartmentType(
                DepartmentType.DEPT_2001, PageRequest.of(0, 10))
            .get(0);
    assertThat(overview.id()).isEqualTo(boardId);
    assertThat(overview.volunteer()).isEqualTo(2);
    assertThat(overview.departmentTypeList()).containsExactly(DepartmentType.DEPT_2001);
    RecruitmentDetails details =
        boards
            .findBoardDetailsByIdAndViewType(boardId, RecruitmentViewType.MEMBER, true)
            .orElseThrow();
    assertThat(details.authorId()).isEqualTo(authorId);
    assertThat(details.author()).isEqualTo("author");
    assertThat(details.title()).isEqualTo("모집");
    assertThat(details.content()).isEqualTo("내용");
    assertThat(details.tags()).isEqualTo("tag");
    assertThat(details.volunteer()).isEqualTo(2);
    assertThat(details.viewType()).isEqualTo(RecruitmentViewType.MEMBER);
    assertThat(details.isAlreadyApplied()).isTrue();
    assertThat(boards.findBoardDetailsByIdAndViewType(-1L, RecruitmentViewType.GUEST, false))
        .isEmpty();
  }

  private ProjectBoard board(String title, LocalDateTime start, LocalDateTime end) {
    ProjectBoard board =
        ProjectBoard.builder()
            .title(title)
            .content("내용")
            .tags("tag")
            .startAt(start)
            .endAt(end)
            .author(author)
            .build();
    em.persist(board);
    em.persist(new ProjectBoardDepartment(board, department));
    return board;
  }

  private Member member(String name) {
    Member member =
        Member.builder().email(name + "@example.com").nickname(name).password("encoded").build();
    em.persist(member);
    return member;
  }

  private void flushAndClear() {
    em.flush();
    em.clear();
  }
}
