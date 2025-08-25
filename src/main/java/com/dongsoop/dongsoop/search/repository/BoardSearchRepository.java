package com.dongsoop.dongsoop.search.repository;

import com.dongsoop.dongsoop.search.entity.BoardDocument;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

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
                        {"term": {"board_type": "marketplace"}}
                    ]
                }
            }
            """)
    Page<BoardDocument> findMarketplaceByKeyword(String keyword, Pageable pageable);

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
                        {"term": {"board_type": "marketplace"}},
                        {"term": {"marketplace_type": "?1"}}
                    ]
                }
            }
            """)
    Page<BoardDocument> findMarketplaceByKeywordAndType(String keyword, String marketplaceType, Pageable pageable);
}