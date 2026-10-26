package com.dongsoop.dongsoop.restaurant.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@Entity
@IdClass(RestaurantLike.RestaurantLikeKey.class)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"restaurant", "member"})
public class RestaurantLike {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "restaurant_id", updatable = false)
    private Restaurant restaurant;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "member_id", updatable = false)
    private Member member;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class RestaurantLikeKey implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long restaurant;

        private Long member;
    }
}
