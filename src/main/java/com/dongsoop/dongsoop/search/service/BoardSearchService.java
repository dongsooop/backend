package com.dongsoop.dongsoop.search.service;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import com.dongsoop.dongsoop.member.service.MemberService;
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
import java.util.Locale;
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
    private final MemberService memberService;

    public Page<BoardDocument> searchByBoardType(String keyword, BoardType boardType, String departmentName,
                                                 Pageable pageable) {
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

    public Page<BoardDocument> searchMarketplace(String keyword, MarketplaceType marketplaceType, Pageable pageable) {
        String processedKeyword = preprocessKeyword(keyword);
        if (processedKeyword.isEmpty()) {
            return Page.empty(pageable);
        }

        updateKeywordRedis(processedKeyword);

        String mpTypeStr = (marketplaceType != null) ? marketplaceType.name() : null;

        try {
            return boardSearchRepository.searchDynamic(
                    processedKeyword,
                    "MARKETPLACE",
                    mpTypeStr,
                    null,
                    null,
                    pageable
            );
        } catch (Exception e) {
            log.error("Error in searchMarketplace keyword='{}'", keyword, e);
            return Page.empty(pageable);
        }
    }

    public SearchResponse<BoardSearchResult> searchNoticesByDepartment(String keyword, String authorName,
                                                                       Pageable pageable) {
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
            log.error("Error in searchNoticesByDepartment keyword='{}'", keyword, e);
            return createEmptySearchResponse(pageable);
        }
    }

    public SearchResponse<RestaurantSearchResult> searchRestaurants(String keyword, Pageable pageable) {
        String processedKeyword = preprocessKeyword(keyword);
        if (processedKeyword.isEmpty()) {
            return new SearchResponse<>(Collections.emptyList(), 0, 0, pageable.getPageNumber(),
                    pageable.getPageSize());
        }

        updateKeywordRedis(processedKeyword);

        try {
            Long currentMemberId = null;
            try {
                if (memberService.isAuthenticated()) {
                    currentMemberId = memberService.getMemberIdByAuthentication();
                }
            } catch (Exception ignored) {
            }
            final Long memberIdFinal = currentMemberId;

            Page<RestaurantDocument> page = restaurantSearchRepository.searchByKeywordDynamic(processedKeyword,
                    pageable);

            List<RestaurantSearchResult> content = page.getContent().stream()
                    .map(doc -> RestaurantSearchResult.from(doc, memberIdFinal))
                    .toList();

            return new SearchResponse<>(
                    content,
                    (int) page.getTotalElements(),
                    page.getTotalPages(),
                    page.getNumber(),
                    page.getSize()
            );
        } catch (Exception e) {
            log.error("Error in searchRestaurants keyword='{}'", keyword, e);
            return new SearchResponse<>(Collections.emptyList(), 0, 0, pageable.getPageNumber(),
                    pageable.getPageSize());
        }
    }

    public List<String> getAutocompleteSuggestions(String keyword, String boardType) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);

        Pageable pageable = PageRequest.of(0, 10);
        List<String> suggestions = new ArrayList<>();

        if ("RESTAURANT".equalsIgnoreCase(boardType)) {
            List<RestaurantDocument> restaurants = restaurantSearchRepository.findAutocompleteSuggestionsDynamic(
                    keyword, pageable);
            suggestions.addAll(restaurants.stream().map(RestaurantDocument::getName).toList());
        } else if (StringUtils.hasText(boardType)) {
            List<BoardDocument> documents = boardSearchRepository.findAutocompleteSuggestionsDynamic(keyword, boardType,
                    pageable);
            suggestions.addAll(documents.stream().map(BoardDocument::getTitle).toList());
        } else {
            List<BoardDocument> documents = boardSearchRepository.findAutocompleteSuggestionsDynamic(keyword, null,
                    PageRequest.of(0, 5));
            suggestions.addAll(documents.stream().map(BoardDocument::getTitle).toList());

            List<RestaurantDocument> restaurants = restaurantSearchRepository.findAutocompleteSuggestionsDynamic(
                    keyword, PageRequest.of(0, 5));
            suggestions.addAll(restaurants.stream().map(RestaurantDocument::getName).toList());
        }

        // 검색어로 시작하는 단어 우선 정렬 로직 복구
        return suggestions.stream()
                .distinct()
                .sorted((s1, s2) -> {
                    boolean s1Starts = s1.toLowerCase(Locale.ROOT).startsWith(lowerKeyword);
                    boolean s2Starts = s2.toLowerCase(Locale.ROOT).startsWith(lowerKeyword);
                    if (s1Starts && !s2Starts) {
                        return -1;
                    }
                    if (!s1Starts && s2Starts) {
                        return 1;
                    }
                    return s1.compareTo(s2);
                })
                .toList();
    }

    private String preprocessKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }
        return keyword.trim().replaceAll("\\s+", " ");
    }

    private void updateKeywordRedis(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        try {
            popularKeywordService.updateKeywordScore(keyword.toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            log.warn("Failed to update popular keyword: {}", keyword, e);
        }
    }

    private SearchResponse<BoardSearchResult> createEmptySearchResponse(Pageable pageable) {
        return SearchDtoMapper.toSearchResponse(Page.empty(pageable));
    }
}