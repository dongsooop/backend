package com.dongsoop.dongsoop.feedback.repository;

import com.dongsoop.dongsoop.feedback.dto.FeedbackDetail;
import com.dongsoop.dongsoop.feedback.entity.QFeedback;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedbackRepositoryCustomImpl implements FeedbackRepositoryCustom {

    private final QFeedback feedback = QFeedback.feedback;
    private final QMember member = QMember.member;

    private JPAQueryFactory queryFactory;

    @Override
    public Optional<FeedbackDetail> searchFeedbackById(Long id) {
        FeedbackDetail result = queryFactory
                .select(Projections.constructor(FeedbackDetail.class,
                        feedback.id,
                        feedback.content,
                        member.id,
                        member.nickname,
                        feedback.createdAt
                ))
                .from(feedback)
                .join(feedback.member, member)
                .where(feedback.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
