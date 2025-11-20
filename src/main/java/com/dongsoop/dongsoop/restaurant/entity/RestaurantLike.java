package com.dongsoop.dongsoop.restaurant.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "id")
public class RestaurantLike {

    @EmbeddedId
    private RestaurantLikeKey id;

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @EqualsAndHashCode
    @ToString(exclude = {"restaurant", "member"})
    public static class RestaurantLikeKey implements Serializable {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false, name = "restaurant_id", updatable = false)
        private Restaurant restaurant;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false, name = "member_id", updatable = false)
        private Member member;
    }
}