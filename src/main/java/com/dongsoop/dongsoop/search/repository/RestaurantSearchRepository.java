package com.dongsoop.dongsoop.search.repository;

import com.dongsoop.dongsoop.search.entity.RestaurantDocument;
import java.util.List;
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
                                    {"match": {"tags": "?0"}}
                                ],
                                "minimum_should_match": 1
                            }
                        },
                        {"term": {"board_type": "RESTAURANT"}}
                    ]
                }
            }
            """)
    Page<RestaurantDocument> searchByKeyword(String keyword, Pageable pageable);

    //식당 이름 자동완성
    @Query("""
            {
                "bool": {
                    "should": [
                        {
                            "match": {
                                "title.autocomplete": {
                                    "query": "?0",
                                    "boost": 10.0
                                }
                            }
                        },
                        {
                            "match": {
                                "title": {
                                    "query": "?0",
                                    "fuzziness": "AUTO",
                                    "boost": 1.0
                                }
                            }
                        },
                        {
                            "match": {
                                "tags.autocomplete": "?0"
                            }
                        }
                    ],
                    "minimum_should_match": 1
                }
            }
            """)
    List<RestaurantDocument> findAutocompleteSuggestions(String keyword, Pageable pageable);
}
