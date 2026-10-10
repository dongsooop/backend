package com.dongsoop.dongsoop.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.feedback.entity.Feedback;
import com.dongsoop.dongsoop.feedback.repository.FeedbackRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationList;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import com.dongsoop.dongsoop.notification.repository.NotificationRepository;
import com.dongsoop.dongsoop.report.dto.ReportResponse;
import com.dongsoop.dongsoop.report.dto.ReportSummaryResponse;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuerydslPaginationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private FeedbackRepository feedbackRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private ReportRepository reportRepository;

    @Test
    void feedbackListsPreserveCompoundNestedSortingAndPageBoundaries() {
        Member alpha = saveMember("alpha");
        Member beta = saveMember("beta");
        Member gamma = saveMember("gamma");
        saveFeedback(alpha, "first");
        saveFeedback(beta, "second");
        saveFeedback(alpha, "third");
        saveFeedback(gamma, "fourth");
        flushAndClear();
        Pageable page = PageRequest.of(1, 2,
                Sort.by(Sort.Order.desc("member.nickname"), Sort.Order.asc("id")));

        assertThat(feedbackRepository.searchAllImprovementSuggestions(page))
                .containsExactly("first", "third");
        assertThat(feedbackRepository.searchAllFeatureRequests(page))
                .containsExactly("feature-first", "feature-third");
    }

    @Test
    void unsortedFeedbackPageIsBoundedAndUnpagedSortReturnsAllRows() {
        Member member = saveMember("feedback-owner");
        saveFeedback(member, "first");
        saveFeedback(member, "second");
        saveFeedback(member, "third");
        flushAndClear();

        assertThat(feedbackRepository.searchAllImprovementSuggestions(PageRequest.of(0, 2)))
                .hasSize(2)
                .allMatch(List.of("first", "second", "third")::contains);
        assertThat(feedbackRepository.searchAllImprovementSuggestions(
                Pageable.unpaged(Sort.by(Sort.Order.desc("id")))))
                .containsExactly("third", "second", "first");
    }

    @Test
    void notificationsSortByJoinedDetailsAndKeepMemberFilterAndPageBoundaries() {
        Member member = saveMember("notification-owner");
        Member other = saveMember("other-owner");
        NotificationDetails first = saveNotification(member, "alpha");
        NotificationDetails second = saveNotification(member, "beta");
        NotificationDetails third = saveNotification(member, "alpha");
        saveNotification(other, "gamma");
        flushAndClear();
        Pageable page = PageRequest.of(1, 1,
                Sort.by(Sort.Order.desc("title"), Sort.Order.asc("id")));

        assertThat(notificationRepository.getMemberNotifications(member.getId(), page))
                .extracting(NotificationList::id)
                .containsExactly(first.getId());
        assertThat(notificationRepository.getMemberNotifications(member.getId(),
                Pageable.unpaged(Sort.by(Sort.Order.desc("id")))))
                .extracting(NotificationList::id)
                .containsExactly(third.getId(), second.getId(), first.getId());
    }

    @Test
    void reportProjectionsPreserveNestedSortingWithExistingJoinAliases() {
        Member alpha = saveMember("report-alpha");
        Member beta = saveMember("report-beta");
        Member gamma = saveMember("report-gamma");
        Member target = saveMember("report-target");
        Report first = saveReport(alpha, target);
        saveReport(beta, target);
        Report third = saveReport(alpha, target);
        saveReport(gamma, target);
        List<Long> expectedIds = List.of(first.getId(), third.getId());
        flushAndClear();
        Pageable page = PageRequest.of(1, 2,
                Sort.by(Sort.Order.desc("reporter.nickname"), Sort.Order.asc("id")));

        assertThat(reportRepository.findSummaryReportsByFilter(ReportFilterType.ALL, page))
                .extracting(ReportSummaryResponse::id)
                .containsExactlyElementsOf(expectedIds);
        assertThat(reportRepository.findDetailedReportsByFilter(ReportFilterType.ALL, page))
                .extracting(ReportResponse::id)
                .containsExactlyElementsOf(expectedIds);
    }

    private Member saveMember(String nickname) {
        Member member = Member.builder()
                .email(nickname + "@dongyang.ac.kr")
                .nickname(nickname)
                .password("password")
                .build();
        entityManager.persist(member);
        return member;
    }

    private void saveFeedback(Member member, String suggestion) {
        entityManager.persist(Feedback.builder()
                .member(member)
                .improvementSuggestions(suggestion)
                .featureRequests("feature-" + suggestion)
                .build());
    }

    private NotificationDetails saveNotification(Member member, String title) {
        NotificationDetails details = NotificationDetails.builder()
                .title(title)
                .body("body")
                .type(NotificationType.NOTICE)
                .value("notice")
                .build();
        entityManager.persist(details);
        entityManager.persist(new MemberNotification(details, member));
        return details;
    }

    private Report saveReport(Member reporter, Member target) {
        Report report = Report.builder()
                .reporter(reporter)
                .targetMember(target)
                .reportType(ReportType.MEMBER)
                .reportReason(ReportReason.OTHER)
                .targetId(target.getId())
                .targetUrl("/test/" + target.getId())
                .build();
        entityManager.persist(report);
        return report;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
