package com.dongsoop.dongsoop.feedback;

import static org.assertj.core.api.Assertions.*;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.feedback.dto.ServiceFeatureFeedback;
import com.dongsoop.dongsoop.feedback.entity.*;
import com.dongsoop.dongsoop.feedback.repository.FeedbackRepositoryCustomImpl;
import com.dongsoop.dongsoop.member.entity.Member;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
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
  FeedbackQueryPersistenceTest.QueryConfig.class,
  com.dongsoop.dongsoop.common.config.JpaAuditingConfig.class
})
class FeedbackQueryPersistenceTest extends AbstractIntegrationTest {
  @Autowired EntityManager em;
  @Autowired FeedbackRepositoryCustomImpl queries;

  @TestConfiguration
  static class QueryConfig {
    @Bean
    JPAQueryFactory queryFactory(EntityManager em) {
      return new JPAQueryFactory(em);
    }
  }

  @Test
  void detailPreservesAuthorContentAndFeaturesAndAllowsAnonymousFeedback() {
    Member member =
        Member.builder().email("author@example.com").nickname("author").password("encoded").build();
    em.persist(member);
    Feedback first = feedback("first", member);
    em.persist(new FeedbackServiceFeature(first, ServiceFeature.MEAL_INFORMATION));
    em.persist(new FeedbackServiceFeature(first, ServiceFeature.NOTICE_ALERT));
    Feedback anonymous = feedback("guest", null);
    Long id = first.getId();
    Long guestId = anonymous.getId();
    Long memberId = member.getId();
    flushAndClear();
    assertThat(queries.searchFeedbackById(id))
        .hasValueSatisfying(
            row -> {
              assertThat(row.improvementSuggestions()).isEqualTo("improve-first");
              assertThat(row.featureRequests()).isEqualTo("request-first");
              assertThat(row.memberId()).isEqualTo(memberId);
              assertThat(row.memberNickname()).isEqualTo("author");
              assertThat(row.serviceFeatureList())
                  .containsExactlyInAnyOrder(
                      ServiceFeature.MEAL_INFORMATION, ServiceFeature.NOTICE_ALERT);
              assertThat(row.createdAt()).isNotNull();
            });
    assertThat(queries.searchFeedbackById(guestId))
        .hasValueSatisfying(
            row -> {
              assertThat(row.memberId()).isNull();
              assertThat(row.memberNickname()).isNull();
              assertThat(row.serviceFeatureList()).isEmpty();
            });
    assertThat(queries.searchFeedbackById(-1L)).isEmpty();
  }

  @Test
  void overviewCountsFeaturesAndReturnsOnlyLatestThreeContentsInSameOrder() {
    Feedback first = feedback("first", null);
    Feedback second = feedback("second", null);
    feedback("third", null);
    feedback("fourth", null);
    em.persist(new FeedbackServiceFeature(first, ServiceFeature.MEAL_INFORMATION));
    em.persist(new FeedbackServiceFeature(second, ServiceFeature.MEAL_INFORMATION));
    em.persist(new FeedbackServiceFeature(first, ServiceFeature.NOTICE_ALERT));
    flushAndClear();
    var result = queries.searchFeedbackOverview();
    assertThat(result.serviceFeatures())
        .containsExactlyInAnyOrder(
            new ServiceFeatureFeedback("학식 정보 확인 서비스", 2L),
            new ServiceFeatureFeedback("교내 공지 알림 서비스", 1L));
    assertThat(result.improvementSuggestions())
        .containsExactly("improve-fourth", "improve-third", "improve-second");
    assertThat(result.featureRequests())
        .containsExactly("request-fourth", "request-third", "request-second");
    PageRequest page = PageRequest.of(1, 2, Sort.by("id").descending());
    assertThat(queries.searchAllImprovementSuggestions(page))
        .containsExactly("improve-second", "improve-first");
    assertThat(queries.searchAllFeatureRequests(page))
        .containsExactly("request-second", "request-first");
  }

  @Test
  void emptyOverviewAndPageHaveNoInventedCountsOrContents() {
    assertThat(queries.searchFeedbackOverview())
        .satisfies(
            row -> {
              assertThat(row.serviceFeatures()).isEmpty();
              assertThat(row.improvementSuggestions()).isEmpty();
              assertThat(row.featureRequests()).isEmpty();
            });
    assertThat(queries.searchAllFeatureRequests(PageRequest.of(0, 10))).isEmpty();
  }

  private Feedback feedback(String key, Member member) {
    Feedback value =
        Feedback.builder()
            .member(member)
            .improvementSuggestions("improve-" + key)
            .featureRequests("request-" + key)
            .build();
    em.persist(value);
    return value;
  }

  private void flushAndClear() {
    em.flush();
    em.clear();
  }
}
