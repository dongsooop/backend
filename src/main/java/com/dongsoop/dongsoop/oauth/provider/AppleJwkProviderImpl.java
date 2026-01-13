package com.dongsoop.dongsoop.oauth.provider;

import com.dongsoop.dongsoop.oauth.dto.AppleJwk;
import com.dongsoop.dongsoop.oauth.exception.InvalidAppleTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppleJwkProviderImpl implements AppleJwkProvider {

    private static final String CACHE_NAME = "appleJwks";
    private static final String APPLE_JWK_CACHE_KEY = "jwks";
    private static final RestTemplate restTemplate = new RestTemplate();

    private static LocalDate lastEvictedDate = LocalDate.now();

    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    @Value("${spring.security.oauth2.client.provider.apple.jwk-set-uri}")
    private String jwtUri;

    @Override
    public Map<String, AppleJwk> getAppleJwkMap() {
        Cache cache = this.cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            Map<String, AppleJwk> cachedJwks = cache.get(APPLE_JWK_CACHE_KEY, Map.class);
            if (cachedJwks != null) {
                return cachedJwks;
            }
        }

        ResponseEntity<Map> response = restTemplate.getForEntity(jwtUri, Map.class);
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            log.error("failed to fetch apple jwk: response body is null");
            throw new InvalidAppleTokenException();
        }

        List<Map<String, Object>> keys = (List<Map<String, Object>>) responseBody.get("keys");
        return keys.stream()
                .map(k -> this.objectMapper.convertValue(k, AppleJwk.class))
                .collect(Collectors.toMap(AppleJwk::kid, jwk -> jwk, (first, second) -> first));
    }

    @Override
    public void evictAppleJwkCache() {
        LocalDate today = LocalDate.now();

        // 오늘 이미 비웠다면 실행하지 않음
        if (lastEvictedDate != null && lastEvictedDate.isEqual(today)) {
            log.info("Apple JWK cache already initialized today. Skipping eviction.");
            return;
        }

        // 캐시 비우기 실행
        Cache cache = this.cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
            lastEvictedDate = today; // 실행 날짜 업데이트
            log.info("Apple JWK cache evicted (At: {})", today);
        }
    }
}
