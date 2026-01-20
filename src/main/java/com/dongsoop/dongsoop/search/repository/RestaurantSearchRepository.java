package com.dongsoop.dongsoop.search.repository;

import com.dongsoop.dongsoop.search.entity.RestaurantDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantSearchRepository extends ElasticsearchRepository<RestaurantDocument, String>,
        RestaurantSearchRepositoryCustom {
}