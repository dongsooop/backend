package com.dongsoop.dongsoop.memberblock.repository;

import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.memberblock.dto.BlockedMember;
import com.dongsoop.dongsoop.memberblock.entity.QMemberBlock;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberBlockRepositoryCustomImpl implements MemberBlockRepositoryCustom {

    private static final QMemberBlock memberBlock = QMemberBlock.memberBlock;
    private static final QMember member = QMember.member;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<BlockedMember> findByBlockerId(Long blockerId) {
        return queryFactory
                .select(Projections.constructor(BlockedMember.class,
                        member.id,
                        member.nickname
                ))
                .from(memberBlock)
                .leftJoin(memberBlock.id.blockedMember, member)
                .where(memberBlock.id.blocker.id.eq(blockerId))
                .fetch();
    }
}
