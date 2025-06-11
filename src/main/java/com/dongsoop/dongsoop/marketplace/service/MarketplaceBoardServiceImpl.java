package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.marketplace.dto.CreateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepositoryCustom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketplaceBoardServiceImpl implements MarketplaceBoardService {

    private final MarketplaceBoardMapper marketplaceBoardMapper;

    private final MarketplaceBoardRepository marketplaceBoardRepository;

    private final MarketplaceBoardRepositoryCustom marketplaceBoardRepositoryCustom;

    public MarketplaceBoard create(CreateMarketplaceBoardRequest request) {
        MarketplaceBoard board = marketplaceBoardMapper.toEntity(request);
        return marketplaceBoardRepository.save(board);
    }

    public List<MarketplaceBoardOverview> getMarketplaceBoards(Pageable pageable) {
        return marketplaceBoardRepositoryCustom.findMarketplaceBoardOverviewByPage(pageable);
    }
}
