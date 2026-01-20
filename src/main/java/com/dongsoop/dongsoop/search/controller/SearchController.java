package com.dongsoop.dongsoop.search.controller;

import com.dongsoop.dongsoop.search.dto.RestaurantSearchResult;
import com.dongsoop.dongsoop.search.dto.SearchDtoMapper;
import com.dongsoop.dongsoop.search.dto.SearchResponse;
import com.dongsoop.dongsoop.search.entity.BoardType;
import com.dongsoop.dongsoop.search.service.BoardSearchService;
import com.dongsoop.dongsoop.search.service.PopularKeywordService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    private final PopularKeywordService popularKeywordService;

    @GetMapping("/by-type")
    public ResponseEntity<SearchResponse> searchByType( // 게시판 타입별 검색
                                                        @RequestParam String keyword,
                                                        @RequestParam BoardType boardType,
                                                        @RequestParam(required = false) String departmentName,
                                                        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        SearchResponse response = SearchDtoMapper.toSearchResponse(
                boardSearchService.searchByBoardType(keyword, boardType, departmentName, pageable));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/department-notice")
    public ResponseEntity<SearchResponse> searchNoticesByDepartment( // 학과 공지 검색
                                                                     @RequestParam String keyword,
                                                                     @RequestParam String authorName,
                                                                     @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        SearchResponse response = boardSearchService.searchNoticesByDepartment(keyword, authorName, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurant")
    public ResponseEntity<SearchResponse<RestaurantSearchResult>> searchRestaurants( // 맛집 검색
                                                                                     @RequestParam String keyword,
                                                                                     @PageableDefault(sort = "contact_count", direction = Sort.Direction.DESC) Pageable pageable) {
        SearchResponse<RestaurantSearchResult> response = boardSearchService.searchRestaurants(keyword, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> getAutocompleteSuggestions( // 자동완성
                                                                    @RequestParam String keyword,
                                                                    @RequestParam(required = false) String boardType) {
        List<String> suggestions = boardSearchService.getAutocompleteSuggestions(keyword, boardType);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<String>> getPopularKeywords() { // 인기 검색어
        List<String> keywords = popularKeywordService.getPopularKeywords();
        return ResponseEntity.ok(keywords);
    }
}