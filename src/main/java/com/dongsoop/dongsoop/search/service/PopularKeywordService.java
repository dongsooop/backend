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

    public void updateKeywordScore(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        redisTemplate.opsForZSet().incrementScore(POPULAR_KEYWORD_KEY, keyword.trim(), 1.0);
    }

    public List<String> getPopularKeywords() {
        Set<String> keywords = redisTemplate.opsForZSet()
                .reverseRange(POPULAR_KEYWORD_KEY, 0, 9);

        if (keywords == null) {
            return List.of();
        }
        return keywords.stream().collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void resetPopularKeywords() {
        redisTemplate.delete(POPULAR_KEYWORD_KEY);
    }
}