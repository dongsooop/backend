package com.dongsoop.dongsoop.oauth.provider;

import com.dongsoop.dongsoop.oauth.dto.AppleJwk;
import com.dongsoop.dongsoop.oauth.exception.InvalidAppleTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppleJwkProviderImpl implements AppleJwkProvider {

    private static final String CACHE_NAME = "appleJwks";
    private static final String APPLE_JWK_CACHE_KEY = "jwks";
    private static final RestTemplate restTemplate;
    private static LocalDate lastEvictedDate = LocalDate.now();

    static {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000); // 5 seconds
        requestFactory.setReadTimeout(5000); // 5 seconds
        restTemplate = new RestTemplate(requestFactory);
    }

    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    @Value("${spring.security.oauth2.client.provider.apple.jwk-set-uri}")
    private String jwtUri;

    @Override
    @Synchronized
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
        if (keys == null || keys.isEmpty()) {
            log.error("failed to fetch apple jwk: keys field is null or empty");
            throw new InvalidAppleTokenException();
        }

        Map<String, AppleJwk> jwkMap = keys.stream()
                .map(k -> this.objectMapper.convertValue(k, AppleJwk.class))
                .collect(Collectors.toMap(AppleJwk::kid, jwk -> jwk, (first, second) -> first));

        if (cache != null) {
            cache.put(APPLE_JWK_CACHE_KEY, jwkMap);
        }

        return jwkMap;
    }

    @Override
    @Synchronized
    public boolean evictAppleJwkCache() {
        LocalDate today = LocalDate.now();

        // 오늘 이미 비웠다면 실행하지 않음
        if (lastEvictedDate != null && lastEvictedDate.isEqual(today)) {
            log.info("Apple JWK cache already initialized today. Skipping eviction.");
            return false;
        }

        Cache cache = this.cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            return false;
        }

        // 캐시 비우기 실행
        cache.clear();
        lastEvictedDate = today; // 실행 날짜 업데이트
        log.info("Apple JWK cache evicted (At: {})", today);

        return true;
    }
}
