package com.dongsoop.dongsoop.marketplace.repository;

import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface MarketplaceBoardRepositoryCustom {

    List<MarketplaceBoardOverview> findMarketplaceBoardOverviewByPage(Pageable pageable);
}
