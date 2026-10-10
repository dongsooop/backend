package com.dongsoop.dongsoop.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class PageableUtilTest {

    private final PageableUtil pageableUtil = new PageableUtil();
    private final EntityPathBase<Object> entity = new EntityPathBase<>(Object.class, "entity");

    @Test
    void appliesCompoundAndNestedSortingInRequestedOrder() {
        JPAQuery<Object> query = new JPAQuery<>().select(entity).from(entity);

        pageableUtil.applySort(query, Sort.by(Sort.Order.desc("owner.name"), Sort.Order.asc("id")), entity);

        PathBuilder<Object> path = new PathBuilder<>(Object.class, "entity");
        JPAQuery<Object> expected = new JPAQuery<>().select(entity).from(entity)
                .orderBy(path.get("owner").getString("name").desc(), path.getNumber("id", Long.class).asc());
        assertThat(query.toString()).isEqualTo(expected.toString());
    }

    @Test
    void unsortedRequestPreservesExistingQuery() {
        JPAQuery<Object> query = new JPAQuery<>().select(entity).from(entity);
        String original = query.toString();

        pageableUtil.applySort(query, Sort.unsorted(), entity);

        assertThat(query.toString()).isEqualTo(original);
    }
}
