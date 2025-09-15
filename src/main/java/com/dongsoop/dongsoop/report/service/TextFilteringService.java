package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.report.dto.TextFilteringRequestDto;
import com.dongsoop.dongsoop.report.dto.TextFilteringResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

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
        TextFilteringRequestDto request = new TextFilteringRequestDto(text);

        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<TextFilteringRequestDto> httpEntity = new HttpEntity<>(request, headers);

            ResponseEntity<TextFilteringResponseDto> response = restTemplate.postForEntity(
                    filteringApiUrl, httpEntity, TextFilteringResponseDto.class);

            TextFilteringResponseDto body = response.getBody();

            if (body == null) {
                log.warn("Text filtering API response is null");
                return false;
            }

            return body.hasProfanity();
        } catch (Exception e) {
            log.error("Failed to call text filtering API", e);
            return false;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + jwtSecretKey);
        return headers;
    }

    private String getSafeString(String value) {
        return Optional.ofNullable(value).orElse("");
    }
}