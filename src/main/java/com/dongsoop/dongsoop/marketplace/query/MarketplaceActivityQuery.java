package com.dongsoop.dongsoop.marketplace.query;

import com.dongsoop.dongsoop.marketplace.query.dto.MarketplaceSummary;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface MarketplaceActivityQuery {
    List<MarketplaceSummary> findOpenedMarketplaceByAuthorIdAndPage(Long memberId, Pageable pageable);
}
