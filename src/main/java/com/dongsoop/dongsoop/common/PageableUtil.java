package com.dongsoop.dongsoop.common;

import com.dongsoop.dongsoop.tutoring.entity.QTutoringBoard;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PageableUtil {

    public OrderSpecifier<?>[] getAllOrderSpecifiers(Sort sort) {
        return sort.stream()
                .map(order -> toOrderSpecifier(order, QTutoringBoard.tutoringBoard))
                .toArray(OrderSpecifier[]::new);
    }

    private OrderSpecifier<?> toOrderSpecifier(Sort.Order order, QTutoringBoard root) {
        PathBuilder<?> entityPath = new PathBuilder<>(root.getType(), root.getMetadata());
        String[] properties = order.getProperty().split("\\.");

        for (int i = 0; i < properties.length - 1; i++) {
            entityPath = entityPath.get(properties[i]);
        }

        String last = properties[properties.length - 1];
        return new OrderSpecifier<>(getOrder(order), entityPath.getComparable(last, Comparable.class));
    }

    private Order getOrder(Sort.Order order) {
        return order.isAscending() ? Order.ASC : Order.DESC;
    }
}
