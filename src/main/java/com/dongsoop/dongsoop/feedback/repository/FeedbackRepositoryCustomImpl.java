package com.dongsoop.dongsoop.feedback.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.feedback.dto.FeedbackDetail;
import com.dongsoop.dongsoop.feedback.dto.FeedbackOverview;
import com.dongsoop.dongsoop.feedback.dto.ServiceFeatureFeedback;
import com.dongsoop.dongsoop.feedback.entity.QFeedback;
import com.dongsoop.dongsoop.feedback.entity.QFeedbackServiceFeature;
import com.dongsoop.dongsoop.feedback.entity.ServiceFeature;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedbackRepositoryCustomImpl implements FeedbackRepositoryCustom {

    private static final Integer CONTENT_LIMIT = 3;
    private static final QFeedback feedback = QFeedback.feedback;
    private static final QFeedbackServiceFeature feedbackServiceFeature = QFeedbackServiceFeature.feedbackServiceFeature;
    private static final QMember member = QMember.member;

    private final JPAQueryFactory queryFactory;
    private final PageableUtil pageableUtil;

    @Override
    public Optional<FeedbackDetail> searchFeedbackById(Long id) {
        FeedbackDetail base = queryFactory
                .select(Projections.constructor(FeedbackDetail.class,
                        feedback.id,
                        feedback.improvementSuggestions,
                        feedback.featureRequests,
                        member.id,
                        member.nickname,
                        feedback.createdAt
                ))
                .from(feedback)
                .join(feedback.member, member)
                .where(feedback.id.eq(id))
                .groupBy(feedback, member)
                .fetchOne();

        if (base == null) {
            return Optional.empty();
        }

        List<ServiceFeature> serviceFeatureList = queryFactory
                .select(feedbackServiceFeature.id.serviceFeature)
                .from(feedbackServiceFeature)
                .join(feedback).on(feedbackServiceFeature.id.feedbackId.eq(feedback.id))
                .where(feedback.id.eq(id))
                .groupBy(feedbackServiceFeature.id.serviceFeature)
                .fetch();

        FeedbackDetail result = base.fromBase(serviceFeatureList);

        return Optional.of(result);
    }

    @Override
    public FeedbackOverview searchFeedbackOverview() {
        List<ServiceFeatureFeedback> serviceFeatureList = queryFactory
                .select(
                        feedbackServiceFeature.id.serviceFeature,
                        feedbackServiceFeature.id.serviceFeature.count()
                )
                .from(feedbackServiceFeature)
                .groupBy(feedbackServiceFeature.id.serviceFeature)
                .fetch()
                .stream()
                .map(tuple -> {
                    String serviceFeature = tuple.get(0, ServiceFeature.class).getDescription();
                    Long count = tuple.get(1, Long.class);
                    return new ServiceFeatureFeedback(serviceFeature, count);
                })
                .toList();

        List<String[]> contentList = queryFactory
                .select(Projections.array(String[].class,
                        feedback.improvementSuggestions,
                        feedback.featureRequests
                ))
                .from(feedback)
                .orderBy(feedback.id.desc())
                .limit(CONTENT_LIMIT)
                .fetch();

        List<String> improvementSuggestions = new ArrayList<>();
        List<String> featureRequests = new ArrayList<>();

        for (String[] contents : contentList) {
            improvementSuggestions.add(contents[0]);
            featureRequests.add(contents[1]);
        }

        return new FeedbackOverview(serviceFeatureList, improvementSuggestions, featureRequests);
    }

    @Override
    public List<String> searchAllImprovementSuggestions(Pageable pageable) {
        return queryFactory
                .select(feedback.improvementSuggestions)
                .from(feedback)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), feedback))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public List<String> searchAllFeatureRequests(Pageable pageable) {
        return queryFactory
                .select(feedback.featureRequests)
                .from(feedback)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), feedback))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
}
