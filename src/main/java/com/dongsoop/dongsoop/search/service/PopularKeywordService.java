package com.dongsoop.dongsoop.search.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopularKeywordService {

    private static final String POPULAR_KEYWORD_KEY = "popular_keywords";
    private final StringRedisTemplate redisTemplate;

    /**
     * 검색어 점수 증가 (+1) 사용자가 검색할 때마다 호출
     */
    public void updateKeywordScore(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        // ZINCRBY: 키워드가 없으면 추가하고 1, 있으면 점수 +1
        redisTemplate.opsForZSet().incrementScore(POPULAR_KEYWORD_KEY, keyword.trim(), 1.0);
    }

    /**
     * 인기 검색어 Top 10 조회 점수가 높은 순으로 10개 반환
     */
    public List<String> getPopularKeywords() {
        // ZREVRANGE: 점수 높은 순으로 0위~9위 조회
        Set<String> keywords = redisTemplate.opsForZSet()
                .reverseRange(POPULAR_KEYWORD_KEY, 0, 9);

        if (keywords == null) {
            return List.of();
        }
        return keywords.stream().collect(Collectors.toList());
    }

    /**
     * 주기적 초기화 (매일 자정 실행) 너무 오래된 키워드가 계속 상위에 남는 것을 방지
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 00시 00분 00초 실행
    public void resetPopularKeywords() {
        redisTemplate.delete(POPULAR_KEYWORD_KEY);
    }
}