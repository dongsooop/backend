package com.dongsoop.dongsoop.search.controller;

import com.dongsoop.dongsoop.search.entity.BoardDocument;
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
    public ResponseEntity<Page<BoardDocument>> searchAll(
            @RequestParam String keyword,
            Pageable pageable) {
        Page<BoardDocument> results = boardSearchService.searchAll(keyword, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/by-type")
    public ResponseEntity<Page<BoardDocument>> searchByType(
            @RequestParam String keyword,
            @RequestParam String boardType,
            Pageable pageable) {
        Page<BoardDocument> results = boardSearchService.searchByBoardType(keyword, boardType, pageable);
        return ResponseEntity.ok(results);
    }
}