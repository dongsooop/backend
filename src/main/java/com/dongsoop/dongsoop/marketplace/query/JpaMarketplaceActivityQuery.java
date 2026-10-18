package com.dongsoop.dongsoop.marketplace.query;

import static com.dongsoop.dongsoop.marketplace.entity.QMarketplaceBoard.marketplaceBoard;
import static com.dongsoop.dongsoop.marketplace.entity.QMarketplaceContact.marketplaceContact;
import static com.dongsoop.dongsoop.marketplace.entity.QMarketplaceImage.marketplaceImage;

import com.dongsoop.dongsoop.marketplace.query.dto.MarketplaceSummary;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaMarketplaceActivityQuery implements MarketplaceActivityQuery {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<MarketplaceSummary> findOpenedMarketplaceByAuthorIdAndPage(Long memberId, Pageable pageable) {
        return queryFactory.select(Projections.constructor(MarketplaceSummary.class,
                        marketplaceBoard.id,
                        marketplaceBoard.title,
                        marketplaceBoard.content,
                        marketplaceBoard.price,
                        marketplaceBoard.createdAt,
                        marketplaceContact.applicant.countDistinct(),
                        marketplaceImage.url,
                        marketplaceBoard.type,
                        marketplaceBoard.status)) // 처음 저장된 이미지 URL 가져오기
                .from(marketplaceBoard)
                .leftJoin(marketplaceContact)
                .on(marketplaceContact.marketplaceId.eq(marketplaceBoard.id))
                .leftJoin(marketplaceImage)
                .on(marketplaceImage.marketplaceBoard.id.eq(marketplaceBoard.id)
                        .and(marketplaceImage.createdAt.eq(getMinCreated()))) // 가장 먼저 저장된 이미지 행 가져오기
                .where(marketplaceBoard.author.id.eq(memberId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(marketplaceBoard.createdAt.desc())
                .groupBy(marketplaceBoard.id,
                        marketplaceBoard.title,
                        marketplaceBoard.content,
                        marketplaceBoard.price,
                        marketplaceBoard.createdAt,
                        marketplaceImage.url,
                        marketplaceBoard.type,
                        marketplaceBoard.status)
                .fetch();
    }

    private JPQLQuery<LocalDateTime> getMinCreated() {
        return JPAExpressions.select(marketplaceImage.createdAt.min())
                .from(marketplaceImage)
                .where(marketplaceImage.marketplaceBoard.eq(marketplaceBoard));
    }
}
