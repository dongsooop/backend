package com.dongsoop.dongsoop.search.service;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import com.dongsoop.dongsoop.search.dto.BoardSearchResult;
import com.dongsoop.dongsoop.search.dto.RestaurantSearchResult;
import com.dongsoop.dongsoop.search.dto.SearchResponse;
import com.dongsoop.dongsoop.search.entity.BoardDocument;
import com.dongsoop.dongsoop.search.entity.BoardType;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Profile("local")
@Service
@RequiredArgsConstructor
public class LocalBoardSearchServiceImpl implements BoardSearchService {

    public Page<BoardDocument> searchByBoardType(String keyword, List<BoardType> boardTypes,
                                                 MarketplaceType marketplaceType, String departmentName,
                                                 Pageable pageable) {
        log.info("searchByBoardType");
        return Page.empty();
    }

    public SearchResponse<BoardSearchResult> searchNoticesByDepartment(String keyword, String authorName,
                                                                       Pageable pageable) {
        log.info("searchNoticesByDepartment keyword='{}', authorName='{}'", keyword, authorName);
        return null;
    }

    public SearchResponse<RestaurantSearchResult> searchRestaurants(String keyword, Pageable pageable) {
        log.info("searchRestaurants keyword='{}'", keyword);
        return null;
    }

    public List<String> getAutocompleteSuggestions(String keyword, String boardType) {
        log.info("Called getAutocompleteSuggestions with keyword='{}', boardType='{}'", keyword, boardType);
        return Collections.emptyList();
    }
}
