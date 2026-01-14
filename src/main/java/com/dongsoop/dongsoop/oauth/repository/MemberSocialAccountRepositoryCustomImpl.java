package com.dongsoop.dongsoop.oauth.repository;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountOverview;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccount;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.entity.QMemberSocialAccount;
import com.dongsoop.dongsoop.role.entity.QMemberRole;
import com.dongsoop.dongsoop.role.entity.Role;
import com.dongsoop.dongsoop.role.entity.RoleType;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberSocialAccountRepositoryCustomImpl implements MemberSocialAccountRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QMemberSocialAccount memberSocialAccount = QMemberSocialAccount.memberSocialAccount;
    private final QMember member = QMember.member;
    private final QMemberRole memberRole = QMemberRole.memberRole;

    @Override
    public Optional<MemberSocialAccountDto> findMemberSocialAccountDTO(String providerId,
                                                                       OAuthProviderType providerType) {
        List<Tuple> rows = queryFactory
                .select(member,
                        memberSocialAccount.id.providerId,
                        memberSocialAccount.id.providerType,
                        memberRole.id.role)
                .from(memberSocialAccount)
                .leftJoin(memberSocialAccount.member, member)
                .leftJoin(memberRole).on(memberRole.id.member.eq(member))
                .where(memberSocialAccount.id.providerId.eq(providerId)
                        .and(memberSocialAccount.id.providerType.eq(providerType)))
                .fetch();

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Tuple firstRow = rows.get(0);
        List<RoleType> roles = rows.stream()
                .map(t -> t.get(memberRole.id.role))
                .filter(Objects::nonNull)
                .map(Role::getRoleType)
                .distinct()
                .collect(Collectors.toList());

        MemberSocialAccountDto dto = new MemberSocialAccountDto(
                firstRow.get(member),
                firstRow.get(memberSocialAccount.id.providerId),
                providerType,
                roles
        );
        return Optional.of(dto);
    }

    @Override
    public Optional<MemberSocialAccount> findByMemberIdAndProviderType(Long memberId,
                                                                       OAuthProviderType providerType) {
        MemberSocialAccount result = queryFactory
                .selectFrom(memberSocialAccount)
                .where(memberSocialAccount.member.id.eq(memberId)
                        .and(memberSocialAccount.id.providerType.eq(providerType)))
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    @Override
    public List<MemberSocialAccountOverview> findAllMemberSocialAccountOverview(Long memberId) {
        return queryFactory.select(Projections.constructor(MemberSocialAccountOverview.class,
                        memberSocialAccount.id.providerType,
                        memberSocialAccount.createdAt
                ))
                .from(memberSocialAccount)
                .where(memberSocialAccount.member.id.eq(memberId))
                .fetch();
    }

    @Override
    public Optional<MemberSocialAccount> findByMemberAndProviderType(Member member, OAuthProviderType providerType) {
        MemberSocialAccount result = queryFactory
                .selectFrom(memberSocialAccount)
                .where(memberSocialAccount.member.eq(member)
                        .and(memberSocialAccount.id.providerType.eq(providerType)))
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    // 회원에 대해 비관적 잠금(PESSIMISTIC_WRITE) 을 획득하고 반환
    @Override
    public Member findAndLockMember(Long memberId) {
        Member lockedMember = queryFactory
                .selectFrom(member)
                .where(member.id.eq(memberId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        if (lockedMember == null) {
            throw new MemberNotFoundException();
        }

        return lockedMember;
    }
}
