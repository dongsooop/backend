package com.dongsoop.dongsoop.feedback.repository;

import com.dongsoop.dongsoop.feedback.dto.FeedbackDetail;
import com.dongsoop.dongsoop.feedback.dto.FeedbackOverview;
import com.dongsoop.dongsoop.feedback.dto.ServiceFeatureFeedback;
import com.dongsoop.dongsoop.feedback.entity.QFeedback;
import com.dongsoop.dongsoop.feedback.entity.QFeedbackServiceFeature;
import com.dongsoop.dongsoop.feedback.entity.ServiceFeature;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.stereotype.Repository;

@Repository
public class FeedbackRepositoryCustomImpl implements FeedbackRepositoryCustom {

    private static final Integer CONTENT_LIMIT = 3;
    private static final QFeedback feedback = QFeedback.feedback;
    private static final QFeedbackServiceFeature feedbackServiceFeature = QFeedbackServiceFeature.feedbackServiceFeature;
    private static final QMember member = QMember.member;

    private final JPAQueryFactory queryFactory;
    private final Querydsl querydsl;

    public FeedbackRepositoryCustomImpl(EntityManager entityManager,
            JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
        this.querydsl = new Querydsl(entityManager,
                new PathBuilder<>(feedback.getType(), feedback.getMetadata()));
    }

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
                .leftJoin(feedback.member, member)
                .where(feedback.id.eq(id))
                .fetchOne();

        if (base == null) {
            return Optional.empty();
        }

        List<ServiceFeature> serviceFeatureList = queryFactory
                .select(feedbackServiceFeature.serviceFeature)
                .from(feedbackServiceFeature)
                .where(feedbackServiceFeature.feedback.id.eq(id))
                .fetch();

        FeedbackDetail result = base.fromBase(serviceFeatureList);

        return Optional.of(result);
    }

    @Override
    public FeedbackOverview searchFeedbackOverview() {
        List<ServiceFeatureFeedback> serviceFeatureList = queryFactory
                .select(
                        feedbackServiceFeature.serviceFeature,
                        feedbackServiceFeature.serviceFeature.count()
                )
                .from(feedbackServiceFeature)
                .groupBy(feedbackServiceFeature.serviceFeature)
                .fetch()
                .stream()
                .map(this::parseServiceFeature)
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
            if (contents == null || contents.length < 2) {
                continue;
            }
            
            improvementSuggestions.add(contents[0]);
            featureRequests.add(contents[1]);
        }

        return new FeedbackOverview(serviceFeatureList, improvementSuggestions, featureRequests);
    }

    @Override
    public List<String> searchAllImprovementSuggestions(Pageable pageable) {
        return querydsl.applyPagination(pageable, queryFactory
                .select(feedback.improvementSuggestions)
                .from(feedback))
                .fetch();
    }

    @Override
    public List<String> searchAllFeatureRequests(Pageable pageable) {
        return querydsl.applyPagination(pageable, queryFactory
                .select(feedback.featureRequests)
                .from(feedback))
                .fetch();
    }

    private ServiceFeatureFeedback parseServiceFeature(Tuple tuple) {
        ServiceFeature serviceFeature = tuple.get(0, ServiceFeature.class);
        if (serviceFeature == null) {
            return null;
        }

        String description = serviceFeature.getDescription();

        Long count = tuple.get(1, Long.class);
        return new ServiceFeatureFeedback(description, count);
    }
}
