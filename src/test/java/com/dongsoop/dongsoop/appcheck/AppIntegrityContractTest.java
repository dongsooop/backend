package com.dongsoop.dongsoop.appcheck;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.dongsoop.dongsoop.appcheck.dto.FirebaseDeviceData;
import com.dongsoop.dongsoop.appcheck.exception.UnknownFirebaseFetchJWKException;
import com.dongsoop.dongsoop.appcheck.web.WebAppCheckServiceImpl;
import com.dongsoop.dongsoop.appcheck.web.exception.WebAppCheckTokenIssuanceException;
import com.dongsoop.dongsoop.jwt.exception.TokenNotFoundException;
import com.dongsoop.dongsoop.recaptcha.exception.ReCaptchaScoreTooLowException;
import com.dongsoop.dongsoop.recaptcha.exception.ReCaptchaVerificationException;
import com.dongsoop.dongsoop.recaptcha.service.ReCaptchaServiceImpl;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

class AppIntegrityContractTest {
  private static final KeyPair KEYS = Jwts.SIG.RS256.keyPair().build();

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" "})
  void appCheckRejectsMissingTokenBeforeKeyLookup(String token) {
    FirebaseAppCheckImpl service = new FirebaseAppCheckImpl();
    assertThatThrownBy(() -> service.validate(token)).isInstanceOf(TokenNotFoundException.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"valid", "issuer", "audience", "signature", "expired", "unknown-key", "malformed"})
  void appCheckVerifiesSignatureIssuerAudienceAndExpiration(String scenario) {
    FirebaseAppCheckImpl service = new FirebaseAppCheckImpl();
    ReflectionTestUtils.setField(service, "projectNumber", "123");
    RSAPublicKey key = (RSAPublicKey) KEYS.getPublic();
    Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    FirebaseDeviceData jwk =
        new FirebaseDeviceData(
            "RSA",
            "sig",
            "RS256",
            "key",
            encoder.encodeToString(key.getModulus().toByteArray()),
            encoder.encodeToString(key.getPublicExponent().toByteArray()),
            "JWT");
    ReflectionTestUtils.setField(service, "cache", Map.of("key", jwk));
    String issuer = "https://firebaseappcheck.googleapis.com/123";
    String audience = "projects/123";
    String kid = "key";
    KeyPair signing = KEYS;
    Instant expiration = Instant.now().plusSeconds(600);
    if (scenario.equals("issuer")) {
      issuer = "wrong";
    }
    if (scenario.equals("audience")) {
      audience = "wrong";
    }
    if (scenario.equals("signature")) {
      signing = Jwts.SIG.RS256.keyPair().build();
    }
    if (scenario.equals("expired")) {
      expiration = Instant.now().minusSeconds(600);
    }
    if (scenario.equals("unknown-key")) {
      kid = "unknown";
    }
    String token = signedToken(kid, issuer, audience, signing, expiration);
    if (scenario.equals("malformed")) {
      token = "not-base64";
    }
    String candidate = token;
    if (scenario.equals("valid")) {
      assertThatCode(() -> service.validate(candidate)).doesNotThrowAnyException();
      return;
    }
    assertThatThrownBy(() -> service.validate(candidate))
        .isInstanceOf(UnknownFirebaseFetchJWKException.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"token\":\"issued-token\"}",
        "{}",
        "{\"token\":null}",
        "{\"token\":4}",
        "{\"token\":\" \"}",
        "null"
      })
  void webAppCheckAcceptsOnlyNonblankStringTokenAndSendsBearer(String response) throws Exception {
    RestTemplate rest = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
    WebAppCheckServiceImpl service = new WebAppCheckServiceImpl(rest, mock(ResourceLoader.class));
    ReflectionTestUtils.setField(service, "projectNumber", "123");
    ReflectionTestUtils.setField(service, "webAppId", "fake-app");
    GoogleCredentials credentials = mock(GoogleCredentials.class);
    when(credentials.getAccessToken()).thenReturn(new AccessToken("fake-access", null));
    ReflectionTestUtils.setField(service, "credentials", credentials);
    server
        .expect(
            requestTo(
                "https://firebaseappcheck.googleapis.com/v1/projects/123/apps/fake-app:generateAppCheckToken"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer fake-access"))
        .andExpect(content().json("{}"))
        .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    if (response.contains("issued-token")) {
      assertThat(service.issue()).isEqualTo("issued-token");
    }
    if (!response.contains("issued-token")) {
      assertThatThrownBy(service::issue).isInstanceOf(WebAppCheckTokenIssuanceException.class);
    }
    verify(credentials).refreshIfExpired();
    server.verify();
  }

  @Test
  void credentialRefreshFailureBecomesIssuanceErrorWithoutHttp() throws Exception {
    RestTemplate rest = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
    WebAppCheckServiceImpl service = new WebAppCheckServiceImpl(rest, mock(ResourceLoader.class));
    GoogleCredentials credentials = mock(GoogleCredentials.class);
    doThrow(new java.io.IOException("refresh unavailable")).when(credentials).refreshIfExpired();
    ReflectionTestUtils.setField(service, "credentials", credentials);
    assertThatThrownBy(service::issue).isInstanceOf(WebAppCheckTokenIssuanceException.class);
    server.verify();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"valid", "boundary", "low", "failure", "action", "no-action", "null", "http"})
  void recaptchaEnforcesActionAndScoreBoundaryAndSendsForm(String scenario) {
    RestTemplate rest = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
    ReCaptchaServiceImpl service = new ReCaptchaServiceImpl(rest);
    ReflectionTestUtils.setField(service, "secretKey", "fake-secret");
    ReflectionTestUtils.setField(service, "verifyUrl", "https://example.com/verify");
    ReflectionTestUtils.setField(service, "scoreThreshold", 0.5);
    var form = new LinkedMultiValueMap<String, String>();
    form.add("secret", "fake-secret");
    form.add("response", "fake-token");
    var expectation =
        server
            .expect(requestTo("https://example.com/verify"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().formData(form));
    String json = "{\"success\":true,\"score\":0.9,\"action\":\"login\"}";
    if (scenario.equals("boundary")) {
      json = json.replace("0.9", "0.5");
    }
    if (scenario.equals("low")) {
      json = json.replace("0.9", "0.49");
    }
    if (scenario.equals("failure")) {
      json = json.replace("true", "false");
    }
    if (scenario.equals("action")) {
      json = json.replace("login", "other");
    }
    if (scenario.equals("null")) {
      json = "null";
    }
    if (scenario.equals("http")) {
      expectation.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    }
    if (!scenario.equals("http")) {
      expectation.andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
    String action = "login";
    if (scenario.equals("no-action")) {
      action = null;
    }
    String requestedAction = action;
    if (scenario.equals("valid") || scenario.equals("boundary") || scenario.equals("no-action")) {
      assertThatCode(() -> service.verify("fake-token", requestedAction))
          .doesNotThrowAnyException();
    }
    if (scenario.equals("low")) {
      assertThatThrownBy(() -> service.verify("fake-token", requestedAction))
          .isInstanceOf(ReCaptchaScoreTooLowException.class);
    }
    if (scenario.equals("failure")
        || scenario.equals("action")
        || scenario.equals("null")
        || scenario.equals("http")) {
      assertThatThrownBy(() -> service.verify("fake-token", requestedAction))
          .isInstanceOf(ReCaptchaVerificationException.class);
    }
    server.verify();
  }

  @SuppressWarnings("java:S2143")
  private String signedToken(
      String kid, String issuer, String audience, KeyPair keys, Instant expiration) {
    return Jwts.builder()
        .header()
        .keyId(kid)
        .and()
        .issuer(issuer)
        .audience()
        .add(audience)
        .and()
        .expiration(java.util.Date.from(expiration))
        .signWith(keys.getPrivate())
        .compact();
  }
}
