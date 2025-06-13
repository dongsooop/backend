package com.dongsoop.dongsoop.marketplace.repository;

import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardDetails;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceViewType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface MarketplaceBoardRepositoryCustom {

    List<MarketplaceBoardOverview> findMarketplaceBoardOverviewByPage(Pageable pageable);

    Optional<MarketplaceBoardDetails> findMarketplaceBoardDetails(Long id, MarketplaceViewType viewType);
}
