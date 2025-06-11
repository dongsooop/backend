package com.dongsoop.dongsoop.marketplace.controller;

import com.dongsoop.dongsoop.marketplace.dto.ApplyMarketplaceRequest;
import com.dongsoop.dongsoop.marketplace.service.MarketplaceApplyService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/marketplace-apply")
@RequiredArgsConstructor
public class MarketplaceApplyController {

    private final MarketplaceApplyService marketplaceApplyService;

    @PostMapping
    public ResponseEntity<Void> applyMarketplace(ApplyMarketplaceRequest request) {
        marketplaceApplyService.apply(request);

        URI uri = URI.create("marketplace-board/" + request.boardId());

        return ResponseEntity.created(uri)
                .build();
    }
}
