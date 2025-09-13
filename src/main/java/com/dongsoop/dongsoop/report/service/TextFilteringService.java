package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.report.dto.TextFilteringRequestDto;
import com.dongsoop.dongsoop.report.dto.TextFilteringResponseDto;
import java.util.Optional;
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
        TextFilteringRequestDto request = new TextFilteringRequestDto(text);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + jwtSecretKey);

            HttpEntity<TextFilteringRequestDto> httpEntity = new HttpEntity<>(request, headers);

            ResponseEntity<TextFilteringResponseDto> response = restTemplate.postForEntity(
                    filteringApiUrl, httpEntity, TextFilteringResponseDto.class);

            TextFilteringResponseDto body = response.getBody();

            if (body == null) {
                log.warn("Text filtering API response is null");
                return false;
            }

            return (body.getTitle() != null && body.getTitle().isHasProfanity()) ||
                    (body.getTags() != null && body.getTags().isHasProfanity()) ||
                    (body.getContent() != null && body.getContent().isHasProfanity());
        } catch (Exception e) {
            log.error("Failed to call text filtering API", e);
            return false;
        }
    }

    private String getSafeString(String value) {
        return Optional.ofNullable(value).orElse("");
    }
}