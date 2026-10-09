package com.dongsoop.dongsoop.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.restaurant.entity.RestaurantLike;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;

class RestaurantLikeKeyTest {

    @Test
    void 복합_키는_식당과_회원_식별자를_직렬화한다() throws Exception {
        RestaurantLike.RestaurantLikeKey key = RestaurantLike.RestaurantLikeKey.builder()
                .restaurant(10L)
                .member(20L)
                .build();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(key);
        }

        RestaurantLike.RestaurantLikeKey restored;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (RestaurantLike.RestaurantLikeKey) input.readObject();
        }

        assertThat(restored).isEqualTo(key);
    }
}
