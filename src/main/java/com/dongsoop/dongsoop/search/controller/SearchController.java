package com.dongsoop.dongsoop.search.controller;

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

    @GetMapping
    public ResponseEntity<SearchResponse> searchAll(
            @RequestParam String keyword,
            Pageable pageable) {
        Page<BoardDocument> results = boardSearchService.searchAll(keyword, pageable);
        SearchResponse response = SearchDtoMapper.toSearchResponse(results);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-type")
    public ResponseEntity<SearchResponse> searchByType(
            @RequestParam String keyword,
            @RequestParam BoardType boardType,
            Pageable pageable) {
        Page<BoardDocument> results = boardSearchService.searchByBoardType(keyword, boardType, pageable);
        SearchResponse response = SearchDtoMapper.toSearchResponse(results);
        return ResponseEntity.ok(response);
    }
}