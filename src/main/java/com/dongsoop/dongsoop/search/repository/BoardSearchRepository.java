package com.dongsoop.dongsoop.search.repository;

import com.dongsoop.dongsoop.search.entity.BoardDocument;
import java.util.List;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardSearchRepository extends ElasticsearchRepository<BoardDocument, String>,
        BoardSearchRepositoryCustom {

    @Query("""
            {
                "bool": {
                    "should": [
                        {"match": {"title": "?0"}},
                        {"match": {"content": "?0"}}
                    ]
                }
            }
            """)
    List<BoardDocument> findByTitleContainingOrContentContaining(String title, String content); // 웜업용
}