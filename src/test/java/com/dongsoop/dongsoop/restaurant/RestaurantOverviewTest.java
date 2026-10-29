package com.dongsoop.dongsoop.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.restaurant.dto.QRestaurantInfo;
import com.dongsoop.dongsoop.restaurant.dto.QRestaurantOverview;
import com.dongsoop.dongsoop.restaurant.dto.QRestaurantStats;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantInfo;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantOverview;
import com.dongsoop.dongsoop.restaurant.dto.RestaurantStats;
import com.dongsoop.dongsoop.restaurant.entity.RestaurantCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.querydsl.core.types.FactoryExpressionUtils;
import com.querydsl.core.types.dsl.Expressions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RestaurantOverviewTest {

  @Test
  @DisplayName("기본 정보와 좋아요 정보를 합쳐 기존의 평평한 JSON 필드를 유지한다")
  void preservesFlatJsonContract() {
    RestaurantOverview result = overview(" GOOD_VALUE, ,DELICIOUS ");
    ObjectMapper mapper = new ObjectMapper();

    JsonNode actual = mapper.valueToTree(result);
    ObjectNode expected =
        mapper
            .createObjectNode()
            .put("id", 7L)
            .put("name", "식당")
            .put("category", "한식")
            .put("distance", 120.0)
            .put("placeUrl", "https://example.com/place")
            .put("externalMapId", "map-7")
            .put("likeCount", 3L)
            .put("isLikedByMe", true);
    expected.putArray("tags").add("GOOD_VALUE").add("DELICIOUS");

    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t"})
  @DisplayName("태그 집계가 null이거나 비어 있으면 빈 목록으로 변환한다")
  void missingTagsBecomeEmptyList(String tags) {
    assertThat(overview(tags).getTags()).isEmpty();
  }

  @Test
  @DisplayName("QueryDSL은 평평한 조회 열을 중첩 생성자에 올바르게 배분한다")
  void querydslMapsFlattenedColumnsToNestedProjection() {
    QRestaurantOverview projection =
        new QRestaurantOverview(
            new QRestaurantInfo(
                Expressions.constant(7L),
                Expressions.constant("식당"),
                Expressions.constant(RestaurantCategory.KOREAN),
                Expressions.constant(120.0),
                Expressions.constant("https://example.com/place"),
                Expressions.constant("map-7")),
            new QRestaurantStats(Expressions.constant(3L), Expressions.constant(true)),
            Expressions.constant("GOOD_VALUE"));

    RestaurantOverview result =
        FactoryExpressionUtils.wrap(projection)
            .newInstance(
                7L,
                "식당",
                RestaurantCategory.KOREAN,
                120.0,
                "https://example.com/place",
                "map-7",
                3L,
                true,
                "GOOD_VALUE");

    assertThat(result.getId()).isEqualTo(7L);
    assertThat(result.getCategory()).isEqualTo("한식");
    assertThat(result.getExternalMapId()).isEqualTo("map-7");
    assertThat(result.getLikeCount()).isEqualTo(3L);
    assertThat(result.getIsLikedByMe()).isTrue();
    assertThat(result.getTags()).containsExactly("GOOD_VALUE");
  }

  private RestaurantOverview overview(String tags) {
    RestaurantInfo info =
        new RestaurantInfo(
            7L, "식당", RestaurantCategory.KOREAN, 120.0, "https://example.com/place", "map-7");
    return new RestaurantOverview(info, new RestaurantStats(3L, true), tags);
  }
}
