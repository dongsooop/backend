package com.dongsoop.dongsoop.search.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.dongsoop.dongsoop.search.entity.BoardDocument;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class BoardSearchRepositoryImpl implements BoardSearchRepositoryCustom {

    private final ElasticsearchOperations operations;

    @Override
    public Page<BoardDocument> searchDynamic(String keyword, String boardType, String marketplaceType,
                                             String departmentName, String authorName, Pageable pageable) { // 동적 검색 구현
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (StringUtils.hasText(keyword)) {
            boolQueryBuilder.must(m -> m
                    .multiMatch(mm -> mm
                            .query(keyword)
                            .fields("title", "content")
                            .type(TextQueryType.CrossFields)
                            .operator(Operator.And)
                    )
            );
        }

        if (StringUtils.hasText(boardType)) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("board_type").value(boardType)));
        }

        if (StringUtils.hasText(marketplaceType)) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("marketplace_type").value(marketplaceType)));
        }

        if (StringUtils.hasText(authorName)) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("author_name").value(authorName)));
        }

        if (StringUtils.hasText(departmentName)) {
            boolQueryBuilder.must(m -> m
                    .bool(b -> b
                            .should(s -> s.term(t -> t.field("department_name.keyword").value(departmentName)))
                            .should(s -> s.match(mat -> mat.field("tags").query(departmentName)))
                            .minimumShouldMatch("1")
                    )
            );
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(pageable)
                .build();

        SearchHits<BoardDocument> searchHits = operations.search(query, BoardDocument.class);
        List<BoardDocument> content = searchHits.stream().map(SearchHit::getContent).toList();

        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }

    @Override
    public List<BoardDocument> findAutocompleteSuggestionsDynamic(String keyword, String boardType,
                                                                  Pageable pageable) { // 자동완성 구현
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        boolQueryBuilder.must(m -> m
                .bool(b -> b
                        .should(s -> s.match(mat -> mat.field("title.autocomplete").query(keyword).boost(10.0f)))
                        .should(s -> s.match(mat -> mat.field("title").query(keyword).fuzziness("AUTO").boost(1.0f)))
                        .should(s -> s.match(mat -> mat.field("tags.autocomplete").query(keyword)))
                        .minimumShouldMatch("1")
                )
        );

        if (StringUtils.hasText(boardType)) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("board_type").value(boardType)));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(pageable)
                .build();

        SearchHits<BoardDocument> searchHits = operations.search(query, BoardDocument.class);
        return searchHits.stream().map(SearchHit::getContent).toList();
    }
}