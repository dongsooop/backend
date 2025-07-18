package com.dongsoop.dongsoop.search.service;

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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardSearchService {

    private static final String ELASTICSEARCH_WARMUP_KEYWORD = "__warmup__";

    private final BoardSearchRepository boardSearchRepository;

    @PostConstruct
    public void warmupRepository() {
        performWarmupOperation();
    }

    public Page<BoardDocument> searchAll(String keyword, Pageable pageable) {
        List<BoardDocument> allResults = executeSearch(keyword);
        return createPageFromList(allResults, pageable);
    }

    public Page<BoardDocument> searchByBoardType(String keyword, BoardType boardType, Pageable pageable) {
        return executeSearchByBoardType(keyword, boardType, pageable);
    }

    private Page<BoardDocument> executeSearchByBoardType(String keyword, BoardType boardType, Pageable pageable) {
        String processedKeyword = preprocessKeyword(keyword);
        if (processedKeyword.isEmpty()) {
            return Page.empty(pageable);
        }

        return performSearchByBoardType(processedKeyword, boardType, pageable);
    }

    private Page<BoardDocument> performSearchByBoardType(String keyword, BoardType boardType, Pageable pageable) {
        try {
            String boardTypeCode = extractBoardTypeCode(boardType);
            return boardSearchRepository.findByKeywordAndBoardType(keyword, boardTypeCode, pageable);
        } catch (Exception e) {
            logSearchError("searchByBoardType", keyword, boardType.getCode(), e);
            return Page.empty(pageable);
        }
    }

    private String extractBoardTypeCode(BoardType boardType) {
        return boardType.getCode();
    }

    private List<BoardDocument> executeSearch(String keyword) {
        String processedKeyword = preprocessKeyword(keyword);
        if (processedKeyword.isEmpty()) {
            return List.of();
        }

        return performSearch(processedKeyword);
    }

    private List<BoardDocument> performSearch(String keyword) {
        try {
            return boardSearchRepository.findByTitleContainingOrContentContaining(keyword, keyword);
        } catch (Exception e) {
            logSearchError("searchAll", keyword, null, e);
            return List.of();
        }
    }

    private void performWarmupOperation() {
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
        if (keyword == null) {
            return "";
        }

        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        return trimmed.replaceAll("\\s+", " ");
    }

    private Page<BoardDocument> createPageFromList(List<BoardDocument> allResults, Pageable pageable) {
        int start = calculateStartIndex(pageable);
        int end = calculateEndIndex(start, pageable, allResults.size());

        if (start > allResults.size()) {
            return new PageImpl<>(List.of(), pageable, allResults.size());
        }

        List<BoardDocument> pageContent = allResults.subList(start, end);
        return new PageImpl<>(pageContent, pageable, allResults.size());
    }

    private int calculateStartIndex(Pageable pageable) {
        return (int) pageable.getOffset();
    }

    private int calculateEndIndex(int start, Pageable pageable, int totalSize) {
        return Math.min(start + pageable.getPageSize(), totalSize);
    }
}