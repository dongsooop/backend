package com.dongsoop.dongsoop.marketplace.repository;

import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoardStatus;
import com.dongsoop.dongsoop.marketplace.entity.QMarketplaceApply;
import com.dongsoop.dongsoop.marketplace.entity.QMarketplaceBoard;
import com.dongsoop.dongsoop.marketplace.entity.QMarketplaceImage;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MarketplaceBoardRepositoryCustomImpl implements MarketplaceBoardRepositoryCustom {

    private static final QMarketplaceBoard marketplaceBoard = QMarketplaceBoard.marketplaceBoard;

    private static final QMarketplaceApply marketplaceApply = QMarketplaceApply.marketplaceApply;

    private static final QMarketplaceImage marketplaceImage = QMarketplaceImage.marketplaceImage;

    private final JPAQueryFactory queryFactory;

    public List<MarketplaceBoardOverview> findMarketplaceBoardOverviewByPage(Pageable pageable) {
        return queryFactory.select(Projections.constructor(MarketplaceBoardOverview.class,
                        marketplaceBoard.id,
                        marketplaceBoard.title,
                        marketplaceBoard.content,
                        marketplaceBoard.price,
                        marketplaceBoard.createdAt,
                        marketplaceApply.id.applicant.countDistinct(),
                        marketplaceImage.id.url.min())) // 한 개만 가져오기
                .from(marketplaceBoard)
                .leftJoin(marketplaceApply)
                .on(marketplaceApply.id.marketplaceId.eq(marketplaceBoard.id))
                .leftJoin(marketplaceImage)
                .on(marketplaceImage.id.marketplaceBoard.id.eq(marketplaceBoard.id))
                .where(marketplaceBoard.status.eq(MarketplaceBoardStatus.SELLING))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(marketplaceBoard.createdAt.desc())
                .groupBy(marketplaceBoard.id,
                        marketplaceBoard.title,
                        marketplaceBoard.content,
                        marketplaceBoard.price,
                        marketplaceBoard.createdAt,
                        marketplaceApply.id.applicant)
                .fetch();
    }
}
