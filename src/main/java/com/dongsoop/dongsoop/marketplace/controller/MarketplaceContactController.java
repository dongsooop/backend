package com.dongsoop.dongsoop.marketplace.controller;

import com.dongsoop.dongsoop.marketplace.dto.ContactMarketplaceRequest;
import com.dongsoop.dongsoop.marketplace.service.MarketplaceContactService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/marketplace-contact")
@RequiredArgsConstructor
public class MarketplaceContactController {

    private final MarketplaceContactService marketplaceContactService;

    @PostMapping
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Map<String, String>> contactMarketplace(@RequestBody ContactMarketplaceRequest request) {
        String roomId = marketplaceContactService.contact(request);
        URI uri = URI.create("/marketplace-board/" + request.boardId());

        Map<String, String> responseBody = Map.of("roomId", roomId);

        return ResponseEntity.created(uri)
                .body(responseBody);
    }
}