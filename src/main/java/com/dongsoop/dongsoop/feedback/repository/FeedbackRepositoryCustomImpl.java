package com.dongsoop.dongsoop.feedback.repository;

import com.dongsoop.dongsoop.feedback.dto.FeedbackDetail;
import com.dongsoop.dongsoop.feedback.entity.QFeedback;
import com.dongsoop.dongsoop.feedback.entity.QFeedbackServiceFeature;
import com.dongsoop.dongsoop.feedback.entity.ServiceFeature;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedbackRepositoryCustomImpl implements FeedbackRepositoryCustom {

    private static final QFeedback feedback = QFeedback.feedback;
    private static final QFeedbackServiceFeature feedbackServiceFeature = QFeedbackServiceFeature.feedbackServiceFeature;
    private static final QMember member = QMember.member;

    private final JPAQueryFactory queryFactory;

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
}
