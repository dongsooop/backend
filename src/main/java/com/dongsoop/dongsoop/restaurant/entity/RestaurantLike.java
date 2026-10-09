package com.dongsoop.dongsoop.restaurant.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "id")
public class RestaurantLike {

    @EmbeddedId
    private RestaurantLikeKey id;

    @MapsId("restaurantId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "restaurant_id", updatable = false)
    private Restaurant restaurant;

    @MapsId("memberId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "member_id", updatable = false)
    private Member member;

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class RestaurantLikeKey implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Column(name = "restaurant_id")
        private Long restaurantId;

        @Column(name = "member_id")
        private Long memberId;
    }
}
