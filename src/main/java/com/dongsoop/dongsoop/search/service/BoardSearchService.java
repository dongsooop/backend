package com.dongsoop.dongsoop.search.service;

import com.dongsoop.dongsoop.marketplace.entity.MarketplaceType;
import com.dongsoop.dongsoop.search.dto.BoardSearchResult;
import com.dongsoop.dongsoop.search.dto.RestaurantSearchResult;
import com.dongsoop.dongsoop.search.dto.SearchResponse;
import com.dongsoop.dongsoop.search.entity.BoardDocument;
import com.dongsoop.dongsoop.search.entity.BoardType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardSearchService {

    Page<BoardDocument> searchByBoardType(String keyword, List<BoardType> boardTypes,
                                          MarketplaceType marketplaceType, String departmentName,
                                          Pageable pageable);

    SearchResponse<BoardSearchResult> searchNoticesByDepartment(String keyword, String authorName,
                                                                Pageable pageable);

    SearchResponse<RestaurantSearchResult> searchRestaurants(String keyword, Pageable pageable);

    List<String> getAutocompleteSuggestions(String keyword, String boardType);
}
