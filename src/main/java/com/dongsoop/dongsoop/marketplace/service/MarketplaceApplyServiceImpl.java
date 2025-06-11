package com.dongsoop.dongsoop.marketplace.service;

import com.dongsoop.dongsoop.marketplace.dto.ApplyMarketplaceRequest;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceApply;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceApplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketplaceApplyServiceImpl implements MarketplaceApplyService {

    private final MarketplaceApplyRepository marketplaceApplyRepository;

    private final MarketplaceApplyMapper marketplaceApplyMapper;

    public void apply(ApplyMarketplaceRequest request) {
        MarketplaceApply marketplaceApply = marketplaceApplyMapper.toEntity(request);
        marketplaceApplyRepository.save(marketplaceApply);
    }
}
