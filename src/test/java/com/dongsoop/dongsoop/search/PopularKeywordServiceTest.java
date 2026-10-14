package com.dongsoop.dongsoop.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.search.service.PopularKeywordService;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class PopularKeywordServiceTest {
    @Mock private StringRedisTemplate redis;
    @Mock private ZSetOperations<String, String> scores;
    private PopularKeywordService service;

    @BeforeEach
    void setUp() {
        service = new PopularKeywordService(redis);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t\n"})
    @DisplayName("빈 인기 검색어는 Redis 점수를 갱신하지 않는다")
    void skipsBlankKeywords(String keyword) {
        service.updateKeywordScore(keyword);
        verifyNoInteractions(redis, scores);
    }

    @Test
    @DisplayName("검색어 앞뒤 공백을 제거하고 인기도를 1 증가시킨다")
    void incrementsTrimmedKeyword() {
        when(redis.opsForZSet()).thenReturn(scores);
        service.updateKeywordScore("  Java Spring  ");
        verify(scores).incrementScore("popular_keywords", "Java Spring", 1.0);
    }

    @Test
    @DisplayName("상위 10개의 검색어를 Redis 순서대로 반환한다")
    void listsTopTenInScoreOrder() {
        when(redis.opsForZSet()).thenReturn(scores);
        when(scores.reverseRange("popular_keywords", 0, 9))
                .thenReturn(new LinkedHashSet<>(List.of("인기 검색어", "다음 검색어")));
        assertEquals(List.of("인기 검색어", "다음 검색어"), service.getPopularKeywords());
    }

    @Test
    @DisplayName("Redis 결과가 없으면 빈 인기 검색어 목록을 반환한다")
    void missingRankingReturnsEmptyList() {
        when(redis.opsForZSet()).thenReturn(scores);
        assertEquals(List.of(), service.getPopularKeywords());
    }

    @Test
    @DisplayName("일별 초기화는 인기 검색어 집합을 삭제한다")
    void resetsDailyRanking() {
        service.resetPopularKeywords();
        verify(redis).delete("popular_keywords");
    }
}
