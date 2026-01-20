package com.dongsoop.dongsoop.search.repository;

import com.dongsoop.dongsoop.search.entity.RestaurantDocument;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RestaurantSearchRepositoryCustom {
    Page<RestaurantDocument> searchByKeywordDynamic(String keyword, Pageable pageable); // 맛집 키워드 검색

    List<RestaurantDocument> findAutocompleteSuggestionsDynamic(String keyword, Pageable pageable); // 맛집 자동완성
}