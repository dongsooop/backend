package com.dongsoop.dongsoop.search.repository;

import com.dongsoop.dongsoop.search.entity.BoardDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardSearchRepository extends ElasticsearchRepository<BoardDocument, String> {

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
    List<BoardDocument> findByTitleContainingOrContentContaining(String title, String content);

    @Query("""
        {
            "bool": {
                "must": [
                    {
                        "bool": {
                            "should": [
                                {"match": {"title": "?0"}},
                                {"match": {"content": "?0"}}
                            ]
                        }
                    },
                    {"term": {"board_type": "?1"}}
                ]
            }
        }
        """)
    Page<BoardDocument> findByKeywordAndBoardType(String keyword, String boardType, Pageable pageable);
}