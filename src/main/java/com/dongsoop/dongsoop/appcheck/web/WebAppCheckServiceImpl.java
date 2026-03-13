package com.dongsoop.dongsoop.appcheck.web;

import com.dongsoop.dongsoop.appcheck.web.exception.WebAppCheckTokenIssuanceException;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class WebAppCheckServiceImpl implements WebAppCheckService {

    private static final String FIREBASE_APP_CHECK_SCOPE = "https://www.googleapis.com/auth/firebase.appcheck";
    private static final String GENERATE_TOKEN_URL_TEMPLATE =
            "https://firebaseappcheck.googleapis.com/v1/projects/%s/apps/%s:generateAppCheckToken";

    private final RestTemplate restTemplate;
    private final ResourceLoader resourceLoader;

    @Value("${firebase.service-account.path}")
    private String serviceAccountPath;

    @Value("${firebase.project.number}")
    private String projectNumber;

    @Value("${firebase.web.app-id}")
    private String webAppId;

    private GoogleCredentials credentials;

    @PostConstruct
    private void initCredentials() throws IOException {
        try (InputStream inputStream = resourceLoader.getResource(serviceAccountPath).getInputStream()) {
            this.credentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(FIREBASE_APP_CHECK_SCOPE);
        }
    }

    @Override
    public String issue() {
        try {
            credentials.refreshIfExpired();
            String accessToken = credentials.getAccessToken().getTokenValue();
            return requestAppCheckToken(accessToken);
        } catch (WebAppCheckTokenIssuanceException e) {
            throw e;
        } catch (Exception e) {
            throw new WebAppCheckTokenIssuanceException(e);
        }
    }

    private String requestAppCheckToken(String accessToken) {
        String url = String.format(GENERATE_TOKEN_URL_TEMPLATE, projectNumber, webAppId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Collections.emptyMap(), headers);

        Map<?, ?> response = restTemplate.postForObject(url, request, Map.class);

        if (response == null || !(response.get("token") instanceof String token) || token.isBlank()) {
            log.error("Firebase App Check token generation failed. response: {}", response);
            throw new WebAppCheckTokenIssuanceException(new IllegalStateException("앱 체크 토큰 응답이 올바르지 않습니다."));
        }

        log.debug("Firebase App Check token issued for web. appId: {}", webAppId);
        return token;
    }
}
