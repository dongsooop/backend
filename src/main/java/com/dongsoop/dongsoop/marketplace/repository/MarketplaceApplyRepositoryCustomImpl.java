package com.dongsoop.dongsoop.marketplace.repository;

import com.dongsoop.dongsoop.marketplace.entity.QMarketplaceApply;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MarketplaceApplyRepositoryCustomImpl implements MarketplaceApplyRepositoryCustom {

    private static final QMarketplaceApply marketplaceApply = QMarketplaceApply.marketplaceApply;

    private final JPAQueryFactory queryFactory;

    public boolean existsByBoardIdAndMemberId(Long boardId, Long memberId) {
        return queryFactory.selectFrom(marketplaceApply)
                .where(marketplaceApply.id.marketplaceId.eq(boardId)
                        .and(marketplaceApply.id.applicant.id.eq(memberId)))
                .fetchFirst() != null;
    }
}
