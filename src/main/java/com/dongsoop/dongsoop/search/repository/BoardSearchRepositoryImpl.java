package com.dongsoop.dongsoop.search.repository;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.dongsoop.dongsoop.search.entity.BoardDocument;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class BoardSearchRepositoryImpl extends AbstractSearchRepository<BoardDocument> implements
        BoardSearchRepositoryCustom {

    private static final String NOTICE_BOARD_TYPE = "NOTICE";

    public BoardSearchRepositoryImpl(ElasticsearchOperations operations) {
        super(operations, BoardDocument.class);
    }

    @Override
    public Page<BoardDocument> searchDynamic(String keyword, List<String> boardTypes, String marketplaceType,
                                             String departmentName, String authorName, Pageable pageable) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        addKeywordFilter(boolQueryBuilder, keyword);
        addBoardTypesFilter(boolQueryBuilder, boardTypes);
        addMarketplaceTypeFilter(boolQueryBuilder, marketplaceType);
        addAuthorNameFilter(boolQueryBuilder, authorName);
        addDepartmentFilter(boolQueryBuilder, departmentName);

        return executeSearch(boolQueryBuilder, pageable);
    }

    @Override
    public List<BoardDocument> findAutocompleteSuggestionsDynamic(String keyword, String boardType,
                                                                  Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        addAutocompleteCriteria(boolQueryBuilder, keyword);

        if (StringUtils.hasText(boardType)) {
            boolQueryBuilder.filter(f ->
                    f.term(t -> t
                            .field("board_type").value(boardType)));
        }

        return executeSearchList(boolQueryBuilder, pageable);
    }

    @Override
    public List<BoardDocument> findNoticeAutocompleteSuggestionsDynamic(String keyword, String authorName,
                                                                        Pageable pageable) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        addAutocompleteCriteria(boolQueryBuilder, keyword);
        boolQueryBuilder.filter(f ->
                f.term(t -> t
                        .field("board_type")
                        .value(NOTICE_BOARD_TYPE)));

        addAuthorNameFilter(boolQueryBuilder, authorName);

        return executeSearchList(boolQueryBuilder, pageable);
    }

    private void addKeywordFilter(BoolQuery.Builder builder, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }

        builder.must(m -> m
                .multiMatch(mm -> mm
                        .query(keyword)
                        .fields("title", "content")
                        .type(TextQueryType.CrossFields)
                        .operator(Operator.And)
                )
        );
    }

    private void addBoardTypesFilter(BoolQuery.Builder builder, List<String> boardTypes) {
        if (boardTypes == null || boardTypes.isEmpty()) {
            return;
        }

        List<FieldValue> fieldValues = boardTypes.stream().map(FieldValue::of).toList();

        builder.filter(f -> f
                .terms(t -> t
                        .field("board_type")
                        .terms(tt -> tt.value(fieldValues))
                )
        );
    }

    private void addMarketplaceTypeFilter(BoolQuery.Builder builder, String marketplaceType) {
        if (!StringUtils.hasText(marketplaceType)) {
            return;
        }

        builder.filter(f -> f
                .term(t -> t
                        .field("marketplace_type")
                        .value(marketplaceType)));
    }

    private void addAuthorNameFilter(BoolQuery.Builder builder, String authorName) {
        if (!StringUtils.hasText(authorName)) {
            return;
        }

        builder.filter(f ->
                f.term(t -> t
                        .field("author_name")
                        .value(authorName)));
    }

    private void addDepartmentFilter(BoolQuery.Builder builder, String departmentName) {
        if (!StringUtils.hasText(departmentName)) {
            return;
        }

        builder.must(m -> m
                .bool(b -> b
                        .should(s -> s
                                .term(t -> t
                                        .field("department_name.keyword")
                                        .value(departmentName)))
                        .should(s -> s
                                .match(mat -> mat
                                        .field("tags")
                                        .query(departmentName)))
                        .minimumShouldMatch("1")
                )
        );
    }
}