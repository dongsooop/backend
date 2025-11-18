package com.dongsoop.dongsoop.restaurant.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.Objects;

@Entity
@SuperBuilder
@NoArgsConstructor
public class RestaurantLike {

    @EmbeddedId
    private RestaurantLikeKey id;

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RestaurantLikeKey implements Serializable {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false, name = "restaurant_id", updatable = false)
        private Restaurant restaurant;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false, name = "member_id", updatable = false)
        private Member member;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RestaurantLikeKey that = (RestaurantLikeKey) o;
            return restaurant.equalsId(that.restaurant) &&
                    Objects.equals(member.getId(), that.member.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(restaurant.getId(), member.getId());
        }
    }
}