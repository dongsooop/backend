package com.dongsoop.dongsoop.marketplace.controller;

import com.dongsoop.dongsoop.exception.domain.marketplace.ToManyImagesForMarketplaceException;
import com.dongsoop.dongsoop.marketplace.dto.CreateMarketplaceBoardRequest;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardDetails;
import com.dongsoop.dongsoop.marketplace.dto.MarketplaceBoardOverview;
import com.dongsoop.dongsoop.marketplace.entity.MarketplaceBoard;
import com.dongsoop.dongsoop.marketplace.service.MarketplaceBoardService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/marketplace-board")
@RequiredArgsConstructor
public class MarketplaceBoardController {

    private static final int MAX_IMAGES = 3;

    private final MarketplaceBoardService marketplaceBoardService;

    @GetMapping
    public ResponseEntity<List<MarketplaceBoardOverview>> getMarketplaceBoards(Pageable pageable) {
        List<MarketplaceBoardOverview> marketplaceBoardOverviewList = marketplaceBoardService.getMarketplaceBoards(
                pageable);

        return ResponseEntity.ok(marketplaceBoardOverviewList);
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<MarketplaceBoardDetails> getMarketplaceBoardDetails(@PathVariable("boardId") Long boardId) {
        MarketplaceBoardDetails marketplaceBoardDetails = marketplaceBoardService.getBoardDetails(boardId);

        return ResponseEntity.ok(marketplaceBoardDetails);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> createMarketplaceBoard(
            @RequestPart("request") @Valid CreateMarketplaceBoardRequest request,
            @RequestPart(value = "image", required = false) MultipartFile[] images) throws IOException {
        if (images.length > MAX_IMAGES) {
            throw new ToManyImagesForMarketplaceException(MAX_IMAGES);
        }
        MarketplaceBoard board = marketplaceBoardService.create(request, images);

        URI uri = URI.create("/marketplace-board/" + board.getId());

        return ResponseEntity.created(uri)
                .build();
    }
}
