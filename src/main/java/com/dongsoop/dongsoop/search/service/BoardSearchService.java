package com.dongsoop.dongsoop.search.service;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import com.dongsoop.dongsoop.search.dto.SearchDtoMapper;
import com.dongsoop.dongsoop.search.dto.SearchResponse;
import com.dongsoop.dongsoop.search.entity.BoardDocument;
import com.dongsoop.dongsoop.search.entity.BoardType;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardSearchService {

    private static final String ELASTICSEARCH_WARMUP_KEYWORD = "__warmup__";

    private final BoardSearchRepository boardSearchRepository;

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

    public Page<BoardDocument> searchByBoardType(String keyword, BoardType boardType, String departmentName, Pageable pageable) {
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

    private Page<BoardDocument> executeSearchByBoardType(String keyword, BoardType boardType, String departmentName, Pageable pageable) {
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
            // BoardType을 소문자로 변환
            String lowerBoardType = boardType.getCode().toLowerCase();
            return boardSearchRepository.findByKeywordAndBoardType(keyword, lowerBoardType, pageable);
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

    private Page<BoardDocument> performMarketplaceSearchByType(String keyword, MarketplaceType marketplaceType, Pageable pageable) {
        try {
            // MarketplaceType을 소문자로 변환 (SELL -> sell, BUY -> buy)
            String lowerMarketplaceType = marketplaceType.name().toLowerCase();
            return boardSearchRepository.findMarketplaceByKeywordAndType(keyword, lowerMarketplaceType, pageable);
        } catch (Exception e) {
            logSearchError("searchMarketplaceByType", keyword, marketplaceType.name(), e);
            return Page.empty(pageable);
        }
    }

    private int calculateEndIndex(int start, Pageable pageable, int totalSize) {
        return Math.min(start + pageable.getPageSize(), totalSize);
    }

    public SearchResponse searchNoticesByDepartment(String keyword, String authorName, Pageable pageable) {
        String processedKeyword = preprocessKeyword(keyword);

        if (processedKeyword.isEmpty()) {
            return createEmptySearchResponse(pageable);
        }

        try {
            Page<BoardDocument> results = boardSearchRepository.findNoticesByKeywordAndAuthorName(processedKeyword, authorName, pageable);
            return SearchDtoMapper.toSearchResponse(results);
        } catch (Exception e) {
            logSearchError("searchNoticesByDepartment", processedKeyword, "notice", e);
            return createEmptySearchResponse(pageable);
        }
    }

    private SearchResponse createEmptySearchResponse(Pageable pageable) {
        Page<BoardDocument> emptyPage = Page.empty(pageable);
        return SearchDtoMapper.toSearchResponse(emptyPage);
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

    private Page<BoardDocument> performSearchByBoardTypeAndDepartmentName(String keyword, BoardType boardType, String departmentName, Pageable pageable) {
        try {
            String lowerBoardType = boardType.getCode().toLowerCase();
            return boardSearchRepository.findByKeywordAndBoardTypeAndDepartmentName(keyword, lowerBoardType, departmentName, pageable);
        } catch (Exception e) {
            logSearchError("searchByBoardTypeAndDepartmentName", keyword, boardType.getCode(), e);
            return Page.empty(pageable);
        }
    }
}