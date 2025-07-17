package com.dongsoop.dongsoop.member.repository;

import static com.querydsl.core.group.GroupBy.groupBy;

import com.dongsoop.dongsoop.member.dto.LoginMemberDetails;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.role.entity.QMemberRole;
import com.dongsoop.dongsoop.role.entity.QRole;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {

    private static final QMember member = QMember.member;
    private static final QMemberRole memberRole = QMemberRole.memberRole;
    private static final QRole role = QRole.role;

    private final JPAQueryFactory queryFactory;

    @Value("${member.nickname.alias.prefix:익명_}")
    private String nicknameAliasPrefix;

    @Override
    public Optional<LoginMemberDetails> findLoginMemberDetailById(Long id) {
        LoginMemberDetails loginMemberDetails = queryFactory
                .from(member)
                .leftJoin(memberRole).on(memberRole.id.member.eq(member))
                .leftJoin(role).on(role.eq(memberRole.id.role))
                .where(eqId(id))
                .transform(
                        groupBy(member.id).list(
                                Projections.constructor(LoginMemberDetails.class,
                                        member.id,
                                        member.nickname,
                                        member.email,
                                        member.department.id,
                                        GroupBy.list(memberRole.id.role.roleType)))
                ).get(0);

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
