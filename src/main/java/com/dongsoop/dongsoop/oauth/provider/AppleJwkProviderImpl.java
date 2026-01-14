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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppleJwkProviderImpl implements AppleJwkProvider {

    private final String appleJWKCacheKey = "jwks";
    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    @Value("${spring.security.oauth2.client.provider.apple.jwk-set-uri}")
    private String jwtUri;

    @Value("${oauth.apple.cache-name}")
    private String cacheName;

    @Override
    @Synchronized
    public Map<String, AppleJwk> getAppleJwkMap() {
        Cache cache = this.cacheManager.getCache(cacheName);
        if (cache != null) {
            Map<String, AppleJwk> cachedJwks = cache.get(appleJWKCacheKey, Map.class);
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
            cache.put(appleJWKCacheKey, jwkMap);
        }

        return jwkMap;
    }

    @Override
    @Synchronized
    public boolean evictAppleJwkCache() {
        LocalDate today = LocalDate.now();

        Cache cache = this.cacheManager.getCache(cacheName);
        if (cache == null) {
            return false;
        }

        // 캐시 비우기 실행
        cache.clear();
        log.info("Apple JWK cache evicted (At: {})", today);

        return true;
    }
}
