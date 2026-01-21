package com.dongsoop.dongsoop.search.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.dongsoop.dongsoop.search.entity.RestaurantDocument;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class RestaurantSearchRepositoryImpl extends AbstractSearchRepository<RestaurantDocument> implements
        RestaurantSearchRepositoryCustom {

    private static final String RESTAURANT_BOARD_TYPE = "RESTAURANT";

    public RestaurantSearchRepositoryImpl(ElasticsearchOperations operations) {
        super(operations, RestaurantDocument.class);
    }

    @Override
    public Page<RestaurantDocument> searchByKeywordDynamic(String keyword, Pageable pageable) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        boolQueryBuilder.filter(f -> f.term(t -> t.field("board_type").value(RESTAURANT_BOARD_TYPE)));

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

        return executeSearch(boolQueryBuilder, pageable);
    }

    @Override
    public List<RestaurantDocument> findAutocompleteSuggestionsDynamic(String keyword, Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        boolQueryBuilder.filter(f -> f.term(t -> t.field("board_type").value(RESTAURANT_BOARD_TYPE)));

        addAutocompleteCriteria(boolQueryBuilder, keyword);

        return executeSearchList(boolQueryBuilder, pageable);
    }
}