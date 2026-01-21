package com.dongsoop.dongsoop.search.service;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import com.dongsoop.dongsoop.member.entity.Member;
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

    public Page<BoardDocument> searchByBoardType(String keyword, List<BoardType> boardTypes,
                                                 MarketplaceType marketplaceType, String departmentName,
                                                 Pageable pageable) {
        String processedKeyword = preprocessKeyword(keyword);
        if (processedKeyword.isEmpty()) {
            return Page.empty(pageable);
        }

        updateKeywordRedis(processedKeyword);

        List<String> boardTypeStrings = (boardTypes != null)
                ? boardTypes.stream().map(bt -> bt.getCode().toUpperCase()).toList()
                : null;

        String marketplaceTypeStr = (marketplaceType != null) ? marketplaceType.name() : null;

        return boardSearchRepository.searchDynamic(
                processedKeyword,
                boardTypeStrings,
                marketplaceTypeStr,
                departmentName,
                null,
                pageable
        );
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
                    Collections.singletonList("NOTICE"),
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
            return createEmptyRestaurantResponse(pageable);
        }

        updateKeywordRedis(processedKeyword);

        try {
            Long currentMemberId = getAuthenticatedMemberId();
            Page<RestaurantDocument> page = restaurantSearchRepository.searchByKeywordDynamic(processedKeyword,
                    pageable);
            return toRestaurantResponse(page, currentMemberId);
        } catch (Exception e) {
            log.error("Error in searchRestaurants keyword='{}'", keyword, e);
            return createEmptyRestaurantResponse(pageable);
        }
    }

    public List<String> getAutocompleteSuggestions(String keyword, String boardType) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);

        if ("RESTAURANT".equalsIgnoreCase(boardType)) {
            return getRestaurantAutocompleteSuggestions(keyword, lowerKeyword);
        }

        if ("NOTICE".equalsIgnoreCase(boardType)) {
            return getNoticeAutocompleteSuggestions(keyword, lowerKeyword);
        }

        if (StringUtils.hasText(boardType)) {
            return getBoardAutocompleteSuggestions(keyword, boardType, lowerKeyword);
        }

        return getAllAutocompleteSuggestions(keyword, lowerKeyword);
    }

    private List<String> getNoticeAutocompleteSuggestions(String keyword, String lowerKeyword) {
        String authorName = getMemberDepartmentName();
        if (authorName == null) {
            return Collections.emptyList();
        }

        List<BoardDocument> results = boardSearchRepository.findNoticeAutocompleteSuggestionsDynamic(
                keyword,
                authorName,
                PageRequest.of(0, 10)
        );
        return sortSuggestions(results.stream().map(BoardDocument::getTitle).toList(), lowerKeyword);
    }

    private List<String> getRestaurantAutocompleteSuggestions(String keyword, String lowerKeyword) {
        List<RestaurantDocument> results = restaurantSearchRepository.findAutocompleteSuggestionsDynamic(keyword,
                PageRequest.of(0, 10));
        return sortSuggestions(results.stream().map(RestaurantDocument::getName).toList(), lowerKeyword);
    }

    private List<String> getBoardAutocompleteSuggestions(String keyword, String boardType, String lowerKeyword) {
        List<BoardDocument> results = boardSearchRepository.findAutocompleteSuggestionsDynamic(keyword, boardType,
                PageRequest.of(0, 10));
        return sortSuggestions(results.stream().map(BoardDocument::getTitle).toList(), lowerKeyword);
    }

    private List<String> getAllAutocompleteSuggestions(String keyword, String lowerKeyword) {
        List<String> suggestions = new ArrayList<>();

        List<BoardDocument> boards = boardSearchRepository.findAutocompleteSuggestionsDynamic(keyword, null,
                PageRequest.of(0, 5));
        suggestions.addAll(boards.stream().map(BoardDocument::getTitle).toList());

        List<RestaurantDocument> restaurants = restaurantSearchRepository.findAutocompleteSuggestionsDynamic(keyword,
                PageRequest.of(0, 5));
        suggestions.addAll(restaurants.stream().map(RestaurantDocument::getName).toList());

        return sortSuggestions(suggestions, lowerKeyword);
    }

    private String getMemberDepartmentName() {
        try {
            if (!memberService.isAuthenticated()) {
                return null;
            }
            Member member = memberService.getMemberReferenceByContext();
            return member.getDepartment().getName();
        } catch (Exception e) {
            log.warn("Failed to get member department", e);
            return null;
        }
    }

    private Long getAuthenticatedMemberId() {
        try {
            if (memberService.isAuthenticated()) {
                return memberService.getMemberIdByAuthentication();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private SearchResponse<RestaurantSearchResult> toRestaurantResponse(Page<RestaurantDocument> page, Long memberId) {
        List<RestaurantSearchResult> content = page.getContent().stream()
                .map(doc -> RestaurantSearchResult.from(doc, memberId))
                .toList();

        return new SearchResponse<>(
                content,
                (int) page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    private List<String> sortSuggestions(List<String> suggestions, String lowerKeyword) {
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

    private SearchResponse<RestaurantSearchResult> createEmptyRestaurantResponse(Pageable pageable) {
        return new SearchResponse<>(Collections.emptyList(), 0, 0, pageable.getPageNumber(), pageable.getPageSize());
    }
}
