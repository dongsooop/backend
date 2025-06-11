package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.marketplace.dto.CreateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface MarketplaceBoardService {

    MarketplaceBoard create(CreateMarketplaceBoardRequest request);

    List<MarketplaceBoardOverview> getMarketplaceBoards(Pageable pageable);
}
