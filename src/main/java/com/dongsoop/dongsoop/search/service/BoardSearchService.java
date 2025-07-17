package com.dongsoop.dongsoop.search.service;

import com.dongsoop.dongsoop.search.entity.BoardDocument;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardSearchService {

    private final BoardSearchRepository boardSearchRepository;

    @PostConstruct
    public void warmupRepository() {
        try {
            boardSearchRepository.count();
            boardSearchRepository.findByTitleContainingOrContentContaining("__warmup__", "__warmup__");
        } catch (Exception ignored) {
        }
    }

    public Page<BoardDocument> searchAll(String keyword, Pageable pageable) {
        List<BoardDocument> allResults = safeSearch(keyword);
        return createPageFromList(allResults, pageable);
    }

    public Page<BoardDocument> searchByBoardType(String keyword, String boardType, Pageable pageable) {
        List<BoardDocument> allResults = safeSearch(keyword);
        List<BoardDocument> filteredResults = allResults.stream()
                .filter(doc -> boardType.equals(doc.getBoardType()))
                .toList();
        return createPageFromList(filteredResults, pageable);
    }

    private List<BoardDocument> safeSearch(String keyword) {
        try {
            resetSearchState();
            String processedKeyword = preprocessKeyword(keyword);
            return boardSearchRepository.findByTitleContainingOrContentContaining(processedKeyword, processedKeyword);
        } catch (Exception e) {
            return List.of();
        }
    }

    private void resetSearchState() {
        try {
            boardSearchRepository.count();
            boardSearchRepository.findByTitleContainingOrContentContaining("__reset__", "__reset__");
        } catch (Exception ignored) {
        }
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
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allResults.size());

        if (start > allResults.size()) {
            return new PageImpl<>(List.of(), pageable, allResults.size());
        }

        List<BoardDocument> pageContent = allResults.subList(start, end);
        return new PageImpl<>(pageContent, pageable, allResults.size());
    }
}