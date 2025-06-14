package com.dongsoop.dongsoop.marketplace.repository;

import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardDetails;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceViewType;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoardStatus;
import com.dongsoop.dongsoop.marketplace.entity.QMarketplaceBoard;
import com.dongsoop.dongsoop.marketplace.entity.QMarketplaceContact;
import com.dongsoop.dongsoop.marketplace.entity.QMarketplaceImage;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MarketplaceBoardRepositoryCustomImpl implements MarketplaceBoardRepositoryCustom {

    private static final QMarketplaceBoard marketplaceBoard = QMarketplaceBoard.marketplaceBoard;

    private static final QMarketplaceContact marketplaceContact = QMarketplaceContact.marketplaceContact;

    private static final QMarketplaceImage marketplaceImage = QMarketplaceImage.marketplaceImage;

    private final JPAQueryFactory queryFactory;

    public List<MarketplaceBoardOverview> findMarketplaceBoardOverviewByPage(Pageable pageable) {
        return queryFactory.select(Projections.constructor(MarketplaceBoardOverview.class,
                        marketplaceBoard.id,
                        marketplaceBoard.title,
                        marketplaceBoard.content,
                        marketplaceBoard.price,
                        marketplaceBoard.createdAt,
                        marketplaceContact.id.applicant.countDistinct(),
                        marketplaceImage.id.url)) // 처음 저장된 이미지 URL 가져오기
                .from(marketplaceBoard)
                .leftJoin(marketplaceContact)
                .on(marketplaceContact.id.marketplaceId.eq(marketplaceBoard.id))
                .leftJoin(marketplaceImage)
                .on(marketplaceImage.id.marketplaceBoard.id.eq(marketplaceBoard.id)
                        .and(marketplaceImage.createdAt.eq(getMinCreated()))) // 가장 먼저 저장된 이미지 행 가져오기
                .where(marketplaceBoard.status.eq(MarketplaceBoardStatus.SELLING))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(marketplaceBoard.createdAt.desc())
                .groupBy(marketplaceBoard.id,
                        marketplaceBoard.title,
                        marketplaceBoard.content,
                        marketplaceBoard.price,
                        marketplaceBoard.createdAt)
                .fetch();
    }

    public Optional<MarketplaceBoardDetails> findMarketplaceBoardDetails(Long id, MarketplaceViewType viewType) {
        MarketplaceBoardDetails result = queryFactory.select(Projections.constructor(MarketplaceBoardDetails.class,
                        marketplaceBoard.id,
                        marketplaceBoard.title,
                        marketplaceBoard.content,
                        marketplaceBoard.price,
                        marketplaceBoard.createdAt,
                        marketplaceContact.id.applicant.countDistinct(),
                        Expressions.stringTemplate("string_agg({0}, ',')", marketplaceImage.id.url),
                        Expressions.constant(viewType)))
                .from(marketplaceBoard)
                .leftJoin(marketplaceContact)
                .on(marketplaceContact.id.marketplaceId.eq(marketplaceBoard.id))
                .leftJoin(marketplaceImage)
                .on(marketplaceImage.id.marketplaceBoard.id.eq(marketplaceBoard.id))
                .where(marketplaceBoard.id.eq(id))
                .groupBy(marketplaceBoard.id,
                        marketplaceBoard.title,
                        marketplaceBoard.content,
                        marketplaceBoard.price,
                        marketplaceBoard.createdAt,
                        marketplaceContact.id.applicant)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * @return 가장 먼저 저장된 이미지의 생성일을 나타내는 JPQL 쿼리
     */
    private JPQLQuery<LocalDateTime> getMinCreated() {
        return JPAExpressions
                .select(marketplaceImage.createdAt.min())
                .from(marketplaceImage)
                .where(marketplaceImage.id.marketplaceBoard.eq(marketplaceBoard));
    }
}
