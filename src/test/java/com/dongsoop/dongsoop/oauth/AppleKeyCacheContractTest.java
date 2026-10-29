package com.dongsoop.dongsoop.oauth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.dongsoop.dongsoop.oauth.dto.AppleJwk;
import com.dongsoop.dongsoop.oauth.exception.InvalidAppleTokenException;
import com.dongsoop.dongsoop.oauth.provider.AppleJwkProviderImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class AppleKeyCacheContractTest {
  private static final String URL = "https://example.com/jwks";
  private final RestTemplate rest = new RestTemplate();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
  private static final String FIRST =
      "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"key-1\",\"use\":\"sig\",\"alg\":\"RS256\",\"n\":\"first\",\"e\":\"AQAB\"},{\"kid\":\"key-1\",\"n\":\"duplicate\"}]}";
  private static final String ROTATED = "{\"keys\":[{\"kid\":\"key-2\",\"n\":\"rotated\"}]}";

  @Test
  void fetchConvertsKeysKeepsFirstDuplicateCachesAndEvictsBeforeRotation() {
    AppleJwkProviderImpl provider = provider(new ConcurrentMapCacheManager("apple-keys"));
    server.expect(requestTo(URL)).andRespond(withSuccess(FIRST, MediaType.APPLICATION_JSON));
    server.expect(requestTo(URL)).andRespond(withSuccess(ROTATED, MediaType.APPLICATION_JSON));
    var first = provider.getAppleJwkMap();
    assertThat(first)
        .containsOnlyKeys("key-1")
        .containsEntry("key-1", new AppleJwk("RSA", "key-1", "sig", "RS256", "first", "AQAB"));
    assertThat(provider.getAppleJwkMap()).isSameAs(first);
    assertThat(provider.evictAppleJwkCache()).isTrue();
    assertThat(provider.getAppleJwkMap()).containsOnlyKeys("key-2");
    server.verify();
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", "{}", "{\"keys\":[]}"})
  void missingBodyOrKeysIsRejectedWithoutCachingFailure(String json) {
    AppleJwkProviderImpl provider = provider(new ConcurrentMapCacheManager("apple-keys"));
    server.expect(requestTo(URL)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    server.expect(requestTo(URL)).andRespond(withSuccess(ROTATED, MediaType.APPLICATION_JSON));
    assertThatThrownBy(provider::getAppleJwkMap).isInstanceOf(InvalidAppleTokenException.class);
    assertThat(provider.getAppleJwkMap()).containsOnlyKeys("key-2");
    server.verify();
  }

  @Test
  void absentCacheStillFetchesKeysAndCannotClaimSuccessfulEviction() {
    AppleJwkProviderImpl provider = provider(mock(CacheManager.class));
    server.expect(requestTo(URL)).andRespond(withSuccess(ROTATED, MediaType.APPLICATION_JSON));
    assertThat(provider.evictAppleJwkCache()).isFalse();
    assertThat(provider.getAppleJwkMap()).containsOnlyKeys("key-2");
    server.verify();
  }

  @Test
  void remoteFailureIsPropagatedAndDoesNotCacheEmptySuccess() {
    AppleJwkProviderImpl provider = provider(new ConcurrentMapCacheManager("apple-keys"));
    server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    server.expect(requestTo(URL)).andRespond(withSuccess(ROTATED, MediaType.APPLICATION_JSON));
    assertThatThrownBy(provider::getAppleJwkMap).isInstanceOf(RestClientException.class);
    assertThat(provider.getAppleJwkMap()).containsOnlyKeys("key-2");
    server.verify();
  }

  private AppleJwkProviderImpl provider(CacheManager cache) {
    AppleJwkProviderImpl value = new AppleJwkProviderImpl(rest, cache, new ObjectMapper());
    ReflectionTestUtils.setField(value, "jwtUri", URL);
    ReflectionTestUtils.setField(value, "cacheName", "apple-keys");
    return value;
  }
}
