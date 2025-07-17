package com.dongsoop.dongsoop.member.repository;

import com.dongsoop.dongsoop.member.dto.LoginMemberDetails;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.role.entity.QMemberRole;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {

    private static final String ROLE_DELIMITER = ",";

    private static final QMember member = QMember.member;
    private static final QMemberRole memberRole = QMemberRole.memberRole;

    private final JPAQueryFactory queryFactory;

    @Value("${member.nickname.alias.prefix:익명_}")
    private String nicknameAliasPrefix;

    @Override
    public Optional<LoginMemberDetails> findLoginMemberDetailById(Long id) {
        LoginMemberDetails loginMemberDetails = queryFactory.select(Projections.constructor(LoginMemberDetails.class,
                        member.id.as("id"),
                        member.nickname.as("nickname"),
                        member.email.as("email"),
                        member.department.id.as("departmentType"),
                        Expressions.stringTemplate("string_agg({0}, '" + ROLE_DELIMITER + "')", memberRole.id.role.roleType)))
                .from(member)
                .leftJoin(memberRole)
                .on(memberRole.id.member.eq(member))
                .where(eqId(id))
                .groupBy(member)
                .fetchOne();

        return Optional.ofNullable(loginMemberDetails);
    }

    @Override
    public long softDelete(Long id, String emailAlias, String passwordAlias) {
        return queryFactory.update(member)
                .set(member.email, emailAlias)
                .set(member.nickname, this.nicknameAliasPrefix + id)
                .set(member.password, passwordAlias)
                .setNull(member.studentId)
                .set(member.isDeleted, true)
                .set(member.updatedAt, LocalDateTime.now())
                .where(member.isDeleted.eq(false)
                        .and(member.id.eq(id)))
                .execute();
    }

    private BooleanExpression eqId(Long id) {
        if (id == null) {
            return null;
        }

        return QMember.member.id.eq(id);
    }
}
