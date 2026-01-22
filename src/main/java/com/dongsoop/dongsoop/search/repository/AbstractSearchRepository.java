package com.dongsoop.dongsoop.search.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

@RequiredArgsConstructor
public abstract class AbstractSearchRepository<T> {

    private final ElasticsearchOperations operations;
    private final Class<T> clazz;

    protected Page<T> executeSearch(BoolQuery.Builder boolQueryBuilder, Pageable pageable) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(pageable)
                .build();

        SearchHits<T> searchHits = operations.search(query, clazz);
        List<T> content = searchHits.stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }

    protected List<T> executeSearchList(BoolQuery.Builder boolQueryBuilder, Pageable pageable) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(pageable)
                .build();

        SearchHits<T> searchHits = operations.search(query, clazz);
        return searchHits.stream()
                .map(SearchHit::getContent)
                .toList();
    }

    protected void addAutocompleteCriteria(BoolQuery.Builder builder, String keyword) {
        builder.must(m -> m
                .bool(b -> b
                        .should(s -> s
                                .match(mat -> mat
                                        .field("title.autocomplete")
                                        .query(keyword)
                                        .boost(10.0f)))
                        .should(s -> s
                                .match(mat -> mat
                                        .field("title")
                                        .query(keyword)
                                        .fuzziness("AUTO")
                                        .boost(1.0f)))
                        .should(s -> s
                                .match(mat -> mat
                                        .field("tags.autocomplete")
                                        .query(keyword)))
                        .minimumShouldMatch("1")
                )
        );
    }
}