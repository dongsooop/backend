package com.dongsoop.dongsoop.marketplace.repository;

import com.dongsoop.dongsoop.marketplace.entity.QMarketplaceContact;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MarketplaceContactRepositoryCustomImpl implements MarketplaceContactRepositoryCustom {

    private static final QMarketplaceContact marketplaceContact = QMarketplaceContact.marketplaceContact;

    private final JPAQueryFactory queryFactory;

    public boolean existsByBoardIdAndMemberId(Long boardId, Long memberId) {
        return queryFactory.selectFrom(marketplaceContact)
                .where(marketplaceContact.id.marketplaceId.eq(boardId)
                        .and(marketplaceContact.id.applicant.id.eq(memberId)))
                .fetchFirst() != null;
    }
}
