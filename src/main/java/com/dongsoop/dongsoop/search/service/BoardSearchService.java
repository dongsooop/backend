package com.dongsoop.dongsoop.search.service;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import com.dongsoop.dongsoop.search.dto.BoardSearchResult;
import com.dongsoop.dongsoop.search.dto.RestaurantSearchResult;
import com.dongsoop.dongsoop.search.dto.SearchDtoMapper;
import com.dongsoop.dongsoop.search.dto.SearchResponse;
import com.dongsoop.dongsoop.search.entity.BoardDocument;
import com.dongsoop.dongsoop.search.entity.BoardType;
import com.dongsoop.dongsoop.search.entity.RestaurantDocument;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardSearchService {

    private final BoardSearchRepository boardSearchRepository;
    private final RestaurantSearchRepository restaurantSearchRepository;
    private final PopularKeywordService popularKeywordService;

    public Page<BoardDocument> searchByBoardType(String keyword, BoardType boardType, String departmentName,
                                                 Pageable pageable) { // 게시판 검색
        String processedKeyword = preprocessKeyword(keyword);
        if (processedKeyword.isEmpty()) {
            return Page.empty(pageable);
        }

        updateKeywordRedis(processedKeyword);

        return boardSearchRepository.searchDynamic(
                processedKeyword,
                boardType.getCode().toUpperCase(),
                null,
                departmentName,
                null,
                pageable
        );
    }

    public Page<BoardDocument> searchMarketplace(String keyword, MarketplaceType marketplaceType,
                                                 Pageable pageable) { // 장터 검색
        String processedKeyword = preprocessKeyword(keyword);
        if (processedKeyword.isEmpty()) {
            return Page.empty(pageable);
        }

        updateKeywordRedis(processedKeyword);

        String mpTypeStr = (marketplaceType != null) ? marketplaceType.name() : null;

        return boardSearchRepository.searchDynamic(
                processedKeyword,
                "MARKETPLACE",
                mpTypeStr,
                null,
                null,
                pageable
        );
    }

    public SearchResponse<BoardSearchResult> searchNoticesByDepartment(String keyword, String authorName,
                                                                       Pageable pageable) { // 학과 공지 검색
        String processedKeyword = preprocessKeyword(keyword);
        if (processedKeyword.isEmpty()) {
            return createEmptySearchResponse(pageable);
        }

        updateKeywordRedis(processedKeyword);

        try {
            Page<BoardDocument> results = boardSearchRepository.searchDynamic(
                    processedKeyword,
                    "NOTICE",
                    null,
                    null,
                    authorName,
                    pageable
            );
            return SearchDtoMapper.toSearchResponse(results);
        } catch (Exception e) {
            log.error("Error in searchNoticesByDepartment", e);
            return createEmptySearchResponse(pageable);
        }
    }

    public SearchResponse<RestaurantSearchResult> searchRestaurants(String keyword, Pageable pageable) { // 맛집 검색
        String processedKeyword = preprocessKeyword(keyword);

        // 빈 검색어일 경우 빈 결과 반환
        if (processedKeyword.isEmpty()) {
            return new SearchResponse<>(
                    Collections.emptyList(),
                    0,
                    0,
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );
        }

        updateKeywordRedis(processedKeyword);

        try {
            Page<RestaurantDocument> page = restaurantSearchRepository.searchByKeywordDynamic(processedKeyword,
                    pageable);

            List<RestaurantSearchResult> content = page.getContent().stream()
                    .map(doc -> RestaurantSearchResult.from(doc, null))
                    .toList();

            return new SearchResponse<>(
                    content,
                    (int) page.getTotalElements(),
                    page.getTotalPages(),
                    page.getNumber(),
                    page.getSize()
            );
        } catch (Exception e) {
            log.error("Error in searchRestaurants", e);
            return new SearchResponse<>(
                    Collections.emptyList(),
                    0,
                    0,
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );
        }
    }

    public List<String> getAutocompleteSuggestions(String keyword, String boardType) { // 자동완성
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        Pageable pageable = PageRequest.of(0, 10);
        List<String> suggestions = new ArrayList<>();

        if ("RESTAURANT".equalsIgnoreCase(boardType)) {
            List<RestaurantDocument> restaurants = restaurantSearchRepository.findAutocompleteSuggestionsDynamic(
                    keyword, pageable);
            return restaurants.stream().map(RestaurantDocument::getName).distinct()
                    .toList();
        }

        if (StringUtils.hasText(boardType)) {
            List<BoardDocument> documents = boardSearchRepository.findAutocompleteSuggestionsDynamic(keyword, boardType,
                    pageable);
            return documents.stream().map(BoardDocument::getTitle).distinct().toList();
        }

        List<BoardDocument> documents = boardSearchRepository.findAutocompleteSuggestionsDynamic(keyword, null,
                PageRequest.of(0, 5));
        suggestions.addAll(documents.stream().map(BoardDocument::getTitle).toList());

        List<RestaurantDocument> restaurants = restaurantSearchRepository.findAutocompleteSuggestionsDynamic(keyword,
                PageRequest.of(0, 5));
        suggestions.addAll(restaurants.stream().map(RestaurantDocument::getName).toList());

        return suggestions.stream().distinct().toList();
    }

    private String preprocessKeyword(String keyword) { // 검색어 전처리
        return (keyword == null) ? "" : keyword.trim();
    }

    private void updateKeywordRedis(String keyword) { // 인기 검색어 갱신
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        try {
            popularKeywordService.updateKeywordScore(keyword);
        } catch (Exception e) {
            log.error("Failed to update popular keyword", e);
        }
    }

    private SearchResponse<BoardSearchResult> createEmptySearchResponse(Pageable pageable) { // 빈 결과 생성
        return SearchDtoMapper.toSearchResponse(Page.empty(pageable));
    }
}