package com.dongsoop.dongsoop.marketplace.controller;

import com.dongsoop.dongsoop.marketplace.dto.CreateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import com.dongsoop.dongsoop.marketplace.service.MarketplaceBoardService;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/marketplace-board")
@RequiredArgsConstructor
public class MarketplaceBoardController {

    private final MarketplaceBoardService marketplaceBoardService;

    @GetMapping
    public ResponseEntity<List<MarketplaceBoardOverview>> getMarketplaceBoards(Pageable pageable) {
        List<MarketplaceBoardOverview> marketplaceBoardOverviewList = marketplaceBoardService.getMarketplaceBoards(
                pageable);

        return ResponseEntity.ok(marketplaceBoardOverviewList);
    }

    @PostMapping
    public ResponseEntity<Void> createMarketplaceBoard(@RequestBody CreateMarketplaceBoardRequest request) {
        MarketplaceBoard board = marketplaceBoardService.create(request);

        URI uri = URI.create("/marketplace-board/" + board.getId());

        return ResponseEntity.created(uri)
                .build();
    }
}
