package com.dongsoop.dongsoop.oauth.repository;

import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.entity.QMemberSocialAccount;
import com.dongsoop.dongsoop.role.entity.QMemberRole;
import com.dongsoop.dongsoop.role.entity.RoleType;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
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

    public Optional<MemberSocialAccountDto> findMemberSocialAccountDTO(String providerId) {
        List<Tuple> rows = queryFactory
                .select(member,
                        memberSocialAccount.id.providerId,
                        memberSocialAccount.id.providerType,
                        memberRole.id.role)
                .from(memberSocialAccount)
                .leftJoin(memberSocialAccount.member, member)
                .leftJoin(memberRole).on(memberRole.id.member.eq(member))
                .where(memberSocialAccount.id.providerId.eq(providerId))
                .fetch();

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Tuple firstRow = rows.get(0);
        OAuthProviderType providerType = firstRow.get(memberSocialAccount.id.providerType);
        List<RoleType> roles = rows.stream()
                .map(r -> Objects.requireNonNull(r.get(memberRole.id.role)).getRoleType())
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
}
