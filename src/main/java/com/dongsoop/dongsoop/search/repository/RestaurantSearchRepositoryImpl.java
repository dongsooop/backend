package com.dongsoop.dongsoop.search.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.dongsoop.dongsoop.search.entity.RestaurantDocument;
import java.util.Collections;
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
public class RestaurantSearchRepositoryImpl implements RestaurantSearchRepositoryCustom {

    private final ElasticsearchOperations operations;

    @Override
    public Page<RestaurantDocument> searchByKeywordDynamic(String keyword, Pageable pageable) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        boolQueryBuilder.filter(f -> f.term(t -> t.field("board_type").value("RESTAURANT")));

        if (StringUtils.hasText(keyword)) {
            boolQueryBuilder.must(m -> m
                    .multiMatch(mm -> mm
                            .query(keyword)
                            .fields("title", "tags")
                            .type(TextQueryType.CrossFields)
                            .operator(Operator.And)
                    )
            );
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(pageable)
                .build();

        SearchHits<RestaurantDocument> searchHits = operations.search(query, RestaurantDocument.class);
        List<RestaurantDocument> content = searchHits.stream().map(SearchHit::getContent).toList();

        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }

    @Override
    public List<RestaurantDocument> findAutocompleteSuggestionsDynamic(String keyword, Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // [수정] must -> filter
        boolQueryBuilder.filter(f -> f.term(t -> t.field("board_type").value("RESTAURANT")));

        boolQueryBuilder.must(m -> m
                .bool(b -> b
                        .should(s -> s.match(mat -> mat.field("title.autocomplete").query(keyword).boost(10.0f)))
                        .should(s -> s.match(mat -> mat.field("title").query(keyword).fuzziness("AUTO").boost(1.0f)))
                        .should(s -> s.match(mat -> mat.field("tags.autocomplete").query(keyword)))
                        .minimumShouldMatch("1")
                )
        );

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(pageable)
                .build();

        SearchHits<RestaurantDocument> searchHits = operations.search(query, RestaurantDocument.class);
        return searchHits.stream().map(SearchHit::getContent).toList();
    }
}