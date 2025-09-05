package com.dongsoop.dongsoop.report.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class TextFilteringService {

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${text.filtering.api.url}")
    private String filteringApiUrl;
    @Value("${jwt.systemkey}")
    private String jwtSecretKey;

    public boolean hasProfanity(String title, String tags, String content) {
        String safeTitle = getSafeString(title);
        String safeTags = getSafeString(tags);
        String safeContent = getSafeString(content);

        String text = String.format("%s | %s | %s", safeTitle, safeTags, safeContent);
        TextFilteringRequest request = new TextFilteringRequest(text);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + jwtSecretKey);

            HttpEntity<TextFilteringRequest> httpEntity = new HttpEntity<>(request, headers);

            ResponseEntity<TextFilteringResponse> response = restTemplate.postForEntity(
                    filteringApiUrl, httpEntity, TextFilteringResponse.class);

            TextFilteringResponse body = response.getBody();

            if (body == null) {
                log.warn("텍스트 필터링 API 응답이 null입니다");
                return false;
            }

            return (body.get제목() != null && body.get제목().isHasProfanity()) ||
                    (body.get태그() != null && body.get태그().isHasProfanity()) ||
                    (body.get본문() != null && body.get본문().isHasProfanity());
        } catch (Exception e) {
            log.error("텍스트 필터링 API 호출 실패", e);
            return false;
        }
    }

    private String getSafeString(String value) {
        if (value != null) {
            return value;
        }
        return "";
    }

    public record TextFilteringRequest(String text) {
    }

    public static class TextFilteringResponse {
        private FieldResult 제목;
        private FieldResult 태그;
        private FieldResult 본문;

        public FieldResult get제목() {
            return 제목;
        }

        public FieldResult get태그() {
            return 태그;
        }

        public FieldResult get본문() {
            return 본문;
        }
    }

    public static class FieldResult {
        private boolean has_profanity;

        public boolean isHasProfanity() {
            return has_profanity;
        }
    }
}
