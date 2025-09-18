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

    @Query("""
            {
                "bool": {
                    "must": [
                        {
                            "bool": {
                                "should": [
                                    {"match": {"title": {"query": "?0", "operator": "and"}}},
                                    {"wildcard": {"title": "*?0*"}},
                                    {"match": {"content": {"query": "?0", "operator": "and"}}},
                                    {"wildcard": {"content": "*?0*"}}
                                ],
                                "minimum_should_match": 1
                            }
                        },
                        {"term": {"board_type": "notice"}},
                        {"term": {"author_name": "?1"}}
                    ]
                }
            }
            """)
    Page<BoardDocument> findNoticesByKeywordAndAuthorName(String keyword, String authorName, Pageable pageable);

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
                        {"term": {"board_type": "?1"}},
                        {"term": {"department_name": "?2"}}
                    ]
                }
            }
            """)
    Page<BoardDocument> findByKeywordAndBoardTypeAndDepartmentName(String keyword, String boardType, String departmentName, Pageable pageable);
}