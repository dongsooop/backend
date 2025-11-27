package com.dongsoop.dongsoop.search.repository;

import com.dongsoop.dongsoop.search.entity.RestaurantDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantSearchRepository extends ElasticsearchRepository<RestaurantDocument, String> {

    @Query("""
            {
                "bool": {
                    "must": [
                        {
                            "bool": {
                                "should": [
                                    {"match": {"title": "?0"}},
                                    {"wildcard": {"title.keyword": "*?0*"}}
                                ]
                            }
                        },
                        {"match": {"board_type": "RESTAURANT"}}
                    ]
                }
            }
            """)
    Page<RestaurantDocument> searchByName(String keyword, Pageable pageable);

    @Query("""
            {
                "bool": {
                    "must": [
                        {
                            "bool": {
                                "should": [
                                    {"match": {"tags": "?0"}},
                                    {"wildcard": {"tags.keyword": "*?0*"}}
                                ]
                            }
                        },
                        {"match": {"board_type": "RESTAURANT"}}
                    ]
                }
            }
            """)
    Page<RestaurantDocument> searchByTag(String keyword, Pageable pageable);
}