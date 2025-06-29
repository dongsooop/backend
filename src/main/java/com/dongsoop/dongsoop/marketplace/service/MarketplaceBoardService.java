package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.marketplace.dto.CreateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardDetails;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import com.dongsoop.dongsoop.marketplace.dto.UpdateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface MarketplaceBoardService {

    MarketplaceBoard create(CreateMarketplaceBoardRequest request, MultipartFile[] images) throws IOException;

    List<MarketplaceBoardOverview> getMarketplaceBoards(Pageable pageable, MarketplaceType type);

    MarketplaceBoardDetails getBoardDetails(Long boardId);

    void delete(Long boardId);

    void update(Long boardId, UpdateMarketplaceBoardRequest request, MultipartFile[] images) throws IOException;
}
