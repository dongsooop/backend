package com.dongsoop.dongsoop.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.entity.Restaurant;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantLike;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantTag;
import com.dongsoop.dongsoop.restaurant.repository.RestaurantRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(RestaurantProjectionIntegrationTest.QueryConfig.class)
class RestaurantProjectionIntegrationTest extends AbstractIntegrationTest {

    @Autowired private RestaurantRepository repository;
    @Autowired private EntityManager entityManager;

    @TestConfiguration
    static class QueryConfig {
        @Bean
        JPAQueryFactory queryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
    }

    @Test
    @DisplayName("PostgreSQL 조회는 중첩 projection에 좋아요 집계와 회원 여부 및 태그를 매핑한다")
    void mapsAggregatesAndMemberLikeFromPostgres() {
        Member member = Member.builder().email("projection@example.com").nickname("조회회원").password("encoded").build();
        entityManager.persist(member);
        Restaurant liked = saveRestaurant("좋아요식당", RestaurantCategory.KOREAN, 200.0,
                List.of(RestaurantTag.GOOD_VALUE, RestaurantTag.DELICIOUS));
        Restaurant other = saveRestaurant("가까운식당", RestaurantCategory.KOREAN, 50.0, List.of());
        entityManager.persist(RestaurantLike.builder().restaurant(liked).member(member).build());
        entityManager.flush();
        Long memberId = member.getId();
        Long likedId = liked.getId();
        Long otherId = other.getId();
        entityManager.clear();

        List<RestaurantOverview> results = repository.findNearbyRestaurants(memberId, null, PageRequest.of(0, 10));

        assertThat(results).extracting(RestaurantOverview::getId).containsExactly(likedId, otherId);
        RestaurantOverview first = results.get(0);
        assertThat(first.getName()).isEqualTo("좋아요식당");
        assertThat(first.getCategory()).isEqualTo("한식");
        assertThat(first.getDistance()).isEqualTo(200.0);
        assertThat(first.getPlaceUrl()).isEqualTo("https://example.com/좋아요식당");
        assertThat(first.getExternalMapId()).isEqualTo("map-좋아요식당");
        assertThat(first.getLikeCount()).isEqualTo(1L);
        assertThat(first.getIsLikedByMe()).isTrue();
        assertThat(first.getTags()).containsExactlyInAnyOrder("GOOD_VALUE", "DELICIOUS");
        assertThat(results.get(1).getLikeCount()).isZero();
        assertThat(results.get(1).getIsLikedByMe()).isFalse();
        assertThat(results.get(1).getTags()).isEmpty();
    }

    @Test
    @DisplayName("비회원 카테고리 조회는 필터와 거리 정렬 및 페이지를 유지한다")
    void guestCategoryFilterAndPagination() {
        Restaurant near = saveRestaurant("가까운한식", RestaurantCategory.KOREAN, 20.0, List.of());
        Restaurant far = saveRestaurant("먼한식", RestaurantCategory.KOREAN, 100.0, List.of());
        saveRestaurant("일식", RestaurantCategory.JAPANESE, 10.0, List.of());
        entityManager.flush();
        Long nearId = near.getId();
        Long farId = far.getId();
        entityManager.clear();

        List<RestaurantOverview> firstPage = repository.findNearbyRestaurants(null, RestaurantCategory.KOREAN,
                PageRequest.of(0, 1));
        List<RestaurantOverview> secondPage = repository.findNearbyRestaurants(null, RestaurantCategory.KOREAN,
                PageRequest.of(1, 1));

        assertThat(firstPage).extracting(RestaurantOverview::getId).containsExactly(nearId);
        assertThat(secondPage).extracting(RestaurantOverview::getId).containsExactly(farId);
        assertThat(firstPage.get(0).getIsLikedByMe()).isFalse();
        assertThat(secondPage.get(0).getIsLikedByMe()).isFalse();
    }

    private Restaurant saveRestaurant(String name, RestaurantCategory category, double distance,
                                     List<RestaurantTag> tags) {
        Restaurant restaurant = Restaurant.builder().name(name).category(category).distance(distance)
                .placeUrl("https://example.com/" + name).externalMapId("map-" + name).tags(tags).build();
        entityManager.persist(restaurant);
        return restaurant;
    }
}
