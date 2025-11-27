package com.dongsoop.dongsoop.search.controller;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import com.dongsoop.dongsoop.search.dto.SearchDtoMapper;
import com.dongsoop.dongsoop.search.dto.SearchResponse;
import com.dongsoop.dongsoop.search.entity.BoardDocument;
import com.dongsoop.dongsoop.search.entity.BoardType;
import com.dongsoop.dongsoop.search.service.BoardSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final BoardSearchService boardSearchService;

    @GetMapping("/by-type")
    public ResponseEntity<SearchResponse> searchByType(
            @RequestParam String keyword,
            @RequestParam BoardType boardType,
            @RequestParam(required = false) String departmentName,
            Pageable pageable) {
        Page<BoardDocument> results = boardSearchService.searchByBoardType(keyword, boardType, departmentName, pageable);
        SearchResponse response = SearchDtoMapper.toSearchResponse(results);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/marketplace")
    public ResponseEntity<SearchResponse> searchMarketplace(
            @RequestParam String keyword,
            @RequestParam(required = false) MarketplaceType marketplaceType,
            Pageable pageable) {
        Page<BoardDocument> results = boardSearchService.searchMarketplace(keyword, marketplaceType, pageable);
        SearchResponse response = SearchDtoMapper.toSearchResponse(results);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/department-notice")
    public ResponseEntity<SearchResponse> searchNoticesByDepartment(
            @RequestParam String keyword,
            @RequestParam String authorName,
            Pageable pageable) {
        SearchResponse response = boardSearchService.searchNoticesByDepartment(keyword, authorName, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurant/name")
    public ResponseEntity<SearchResponse> searchRestaurantsByName(
            @RequestParam String keyword, Pageable pageable) {
        SearchResponse response = boardSearchService.searchRestaurantsByName(keyword, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurant/tag")
    public ResponseEntity<SearchResponse> searchRestaurantsByTag(
            @RequestParam String keyword, Pageable pageable) {
        SearchResponse response = boardSearchService.searchRestaurantsByTag(keyword, pageable);
        return ResponseEntity.ok(response);
    }
}