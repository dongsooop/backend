package com.dongsoop.dongsoop.search.service;

import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
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
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardSearchService {

    private static final String ELASTICSEARCH_WARMUP_KEYWORD = "__warmup__";

    private final BoardSearchRepository boardSearchRepository;
    private final RestaurantSearchRepository restaurantSearchRepository;
    private final MemberService memberService;

    @PostConstruct
    public void warmupRepository() {
        try {
            boardSearchRepository.count();
            boardSearchRepository.findByTitleContainingOrContentContaining(
                    ELASTICSEARCH_WARMUP_KEYWORD,
                    ELASTICSEARCH_WARMUP_KEYWORD
            );
        } catch (Exception e) {
            logWarmupError(e);
        }
    }

    public SearchResponse<RestaurantSearchResult> searchRestaurants(String keyword, Pageable pageable) {
        String processedKeyword = preprocessKeyword(keyword);
        if (processedKeyword.isEmpty()) {
            return createEmptySearchResponse(pageable);
        }

        Long currentMemberId = null;
        try {
            currentMemberId = memberService.getMemberIdByAuthentication();
        } catch (NotAuthenticationException e) {
        }

        try {
            Pageable sortedPageable = createLikeSortPageable(pageable);
            Page<RestaurantDocument> results = restaurantSearchRepository.searchByKeyword(processedKeyword,
                    sortedPageable);

            return toRestaurantSearchResponse(results, currentMemberId);
        } catch (Exception e) {
            logSearchError("searchRestaurants", processedKeyword, "restaurant", e);
            return createEmptySearchResponse(pageable);
        }
    }

    private Pageable createLikeSortPageable(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "likeCount")
        );
    }

    private SearchResponse<RestaurantSearchResult> toRestaurantSearchResponse(Page<RestaurantDocument> results,
                                                                              Long memberId) {
        List<RestaurantSearchResult> dtos = results.getContent().stream()
                .map(doc -> RestaurantSearchResult.from(doc, memberId))
                .toList();

        return new SearchResponse<>(
                dtos,
                (int) results.getTotalElements(),
                results.getTotalPages(),
                results.getNumber(),
                results.getSize()
        );
    }

    public Page<BoardDocument> searchByBoardType(String keyword, BoardType boardType, String departmentName,
                                                 Pageable pageable) {
        return executeSearchByBoardType(keyword, boardType, departmentName, pageable);
    }

    public Page<BoardDocument> searchMarketplace(String keyword, MarketplaceType marketplaceType, Pageable pageable) {
        String processedKeyword = preprocessKeyword(keyword);
        if (processedKeyword.isEmpty()) {
            return Page.empty(pageable);
        }

        if (marketplaceType != null) {
            return performMarketplaceSearchByType(processedKeyword, marketplaceType, pageable);
        }

        return performMarketplaceSearch(processedKeyword, pageable);
    }

    public SearchResponse<BoardSearchResult> searchNoticesByDepartment(String keyword, String authorName,
                                                                       Pageable pageable) {
        String processedKeyword = preprocessKeyword(keyword);

        if (processedKeyword.isEmpty()) {
            return createEmptySearchResponse(pageable);
        }

        try {
            Page<BoardDocument> results = boardSearchRepository.findNoticesByKeywordAndAuthorName(processedKeyword,
                    authorName, pageable);
            return SearchDtoMapper.toSearchResponse(results);
        } catch (Exception e) {
            logSearchError("searchNoticesByDepartment", processedKeyword, "notice", e);
            return createEmptySearchResponse(pageable);
        }
    }

    private Page<BoardDocument> executeSearchByBoardType(String keyword, BoardType boardType, String departmentName,
                                                         Pageable pageable) {
        String processedKeyword = preprocessKeyword(keyword);
        if (processedKeyword.isEmpty()) {
            return Page.empty(pageable);
        }

        if (departmentName == null || departmentName.isEmpty()) {
            return performSearchByBoardType(processedKeyword, boardType, pageable);
        }

        return performSearchByBoardTypeAndDepartmentName(processedKeyword, boardType, departmentName, pageable);
    }

    private Page<BoardDocument> performSearchByBoardType(String keyword, BoardType boardType, Pageable pageable) {
        try {
            String upperBoardType = boardType.getCode().toUpperCase();
            return boardSearchRepository.findByKeywordAndBoardType(keyword, upperBoardType, pageable);
        } catch (Exception e) {
            logSearchError("searchByBoardType", keyword, boardType.getCode(), e);
            return Page.empty(pageable);
        }
    }

    private Page<BoardDocument> performMarketplaceSearch(String keyword, Pageable pageable) {
        try {
            return boardSearchRepository.findMarketplaceByKeyword(keyword, pageable);
        } catch (Exception e) {
            logSearchError("searchMarketplace", keyword, null, e);
            return Page.empty(pageable);
        }
    }

    private Page<BoardDocument> performMarketplaceSearchByType(String keyword, MarketplaceType marketplaceType,
                                                               Pageable pageable) {
        try {
            String lowerMarketplaceType = marketplaceType.name().toLowerCase();
            return boardSearchRepository.findMarketplaceByKeywordAndType(keyword, lowerMarketplaceType, pageable);
        } catch (Exception e) {
            logSearchError("searchMarketplaceByType", keyword, marketplaceType.name(), e);
            return Page.empty(pageable);
        }
    }

    private Page<BoardDocument> performSearchByBoardTypeAndDepartmentName(String keyword, BoardType boardType,
                                                                          String departmentName, Pageable pageable) {
        try {
            String upperBoardType = boardType.getCode().toUpperCase();
            return boardSearchRepository.findByKeywordAndBoardTypeAndDepartmentName(keyword, upperBoardType,
                    departmentName, pageable);
        } catch (Exception e) {
            logSearchError("searchByBoardTypeAndDepartmentName", keyword, boardType.getCode(), e);
            return Page.empty(pageable);
        }
    }

    private <T> SearchResponse<T> createEmptySearchResponse(Pageable pageable) {
        return new SearchResponse<>(List.of(), 0, 0, pageable.getPageNumber(), pageable.getPageSize());
    }

    private void logSearchError(String operation, String keyword, String boardType, Exception e) {
        if (boardType != null) {
            log.error("Search operation failed - operation: {}, keyword: {}, boardType: {}",
                    operation, keyword, boardType, e);
            return;
        }
        log.error("Search operation failed - operation: {}, keyword: {}", operation, keyword, e);
    }

    private void logWarmupError(Exception e) {
        log.warn("Elasticsearch warmup operation failed", e);
    }

    private String preprocessKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }

        return keyword.trim().replaceAll("\\s+", " ");
    }
}