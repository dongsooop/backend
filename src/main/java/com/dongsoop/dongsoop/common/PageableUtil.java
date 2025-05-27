package com.dongsoop.dongsoop.common;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PageableUtil {

    public OrderSpecifier<?>[] getAllOrderSpecifiers(Sort sort, EntityPathBase<?> entityPathBase) {
        return sort.stream()
                .map(order -> toOrderSpecifier(order, entityPathBase))
                .toArray(OrderSpecifier[]::new);
    }

    private OrderSpecifier<?> toOrderSpecifier(Sort.Order order, EntityPathBase<?> entityPathBase) {
        PathBuilder<?> entityPath = new PathBuilder<>(entityPathBase.getType(), entityPathBase.getMetadata());
        String[] properties = order.getProperty().split("\\.");

        for (int i = 0; i < properties.length - 1; i++) {
            entityPath = entityPath.get(properties[i]);
        }

        String last = properties[properties.length - 1];
        return new OrderSpecifier<>(getOrder(order), entityPath.getComparable(last, Comparable.class));
    }

    private Order getOrder(Sort.Order order) {
        if (order.isDescending()) {
            return Order.DESC;
        }

        return Order.ASC;
    }
}
