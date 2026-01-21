package com.dongsoop.dongsoop.search.repository;

import com.dongsoop.dongsoop.search.entity.BoardDocument;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardSearchRepositoryCustom {
    Page<BoardDocument> searchDynamic(String keyword, List<String> boardTypes, String marketplaceType,
                                      String departmentName, String authorName, Pageable pageable);

    List<BoardDocument> findAutocompleteSuggestionsDynamic(String keyword, String boardType, Pageable pageable);
}