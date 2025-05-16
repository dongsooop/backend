package com.dongsoop.dongsoop.member.repository;

import com.dongsoop.dongsoop.member.dto.LoginMemberDetails;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {

    private static final QMember member = QMember.member;

    private final JPAQueryFactory queryFactory;

    public Optional<LoginMemberDetails> findLoginMemberDetailById(Long id) {
        LoginMemberDetails loginMemberDetails = queryFactory.select(Projections.constructor(LoginMemberDetails.class,
                        member.id.as("id"),
                        member.nickname.as("nickname"),
                        member.email.as("email"),
                        member.department.id.as("departmentType")))
                .from(member)
                .where(eqId(id))
                .fetchOne();

        return Optional.ofNullable(loginMemberDetails);
    }

    private BooleanExpression eqId(Long id) {
        if (id == null) {
            return null;
        }

        return QMember.member.id.eq(id);
    }
}
