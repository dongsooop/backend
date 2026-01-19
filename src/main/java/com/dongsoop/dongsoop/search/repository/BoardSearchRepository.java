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
                        {"term": {"board_type": "MARKETPLACE"}}
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
                        {"term": {"board_type": "MARKETPLACE"}},
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
                            "multi_match": {
                                "query": "?0",
                                "fields": ["title", "content"],
                                "type": "cross_fields",
                                "operator": "and"
                            }
                        },
                        {"term": {"board_type": "NOTICE"}},
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
                            "multi_match": {
                                "query": "?0",
                                "fields": ["title", "content"],
                                "type": "cross_fields",
                                "operator": "and"
                            }
                        },
                        {
                            "term": {"board_type": "?1"}
                        },
                        {
                            "bool": {
                                "should": [
                                    {"term": {"department_name.keyword": "?2"}},
                                    {"match": {"tags": "?2"}}
                                ],
                                "minimum_should_match": 1
                            }
                        }
                    ]
                }
            }
            """)
    Page<BoardDocument> findByKeywordAndBoardTypeAndDepartmentName(String keyword, String boardType,
                                                                   String departmentName, Pageable pageable);

    //자동완성 검색 (Prefix + Fuzzy + 가중치 적용)
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
    List<BoardDocument> findAutocompleteSuggestions(String keyword, Pageable pageable);
}