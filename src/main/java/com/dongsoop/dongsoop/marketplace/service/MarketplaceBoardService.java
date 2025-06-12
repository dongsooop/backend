package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.marketplace.dto.CreateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface MarketplaceBoardService {

    MarketplaceBoard create(CreateMarketplaceBoardRequest request, MultipartFile image) throws IOException;

    List<MarketplaceBoardOverview> getMarketplaceBoards(Pageable pageable);
}
