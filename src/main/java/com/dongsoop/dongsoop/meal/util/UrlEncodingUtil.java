package com.dongsoop.dongsoop.meal.util;

import com.dongsoop.dongsoop.exception.domain.meal.MealCrawlingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Component
@Slf4j
public class UrlEncodingUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final String ENCODING_PREFIX = "fnct1|@@|";

    public String buildWeekUrl(String baseUrl, LocalDate monday, String weekParam) {
        String mondayStr = monday.format(DATE_FORMATTER);
        String pathOnly = String.format("/diet/dmu/13/view.do?monday=%s&week=%s&", mondayStr, weekParam);

        return createEncodedUrl(baseUrl, pathOnly);
    }

    private String createEncodedUrl(String baseUrl, String pathOnly) {
        try {
            String urlEncodedPath = URLEncoder.encode(pathOnly, StandardCharsets.UTF_8);
            String fullPath = ENCODING_PREFIX + urlEncodedPath;
            String base64Encoded = Base64.getEncoder().encodeToString(fullPath.getBytes(StandardCharsets.UTF_8));
            String finalEncoded = URLEncoder.encode(base64Encoded, StandardCharsets.UTF_8);

            return baseUrl + finalEncoded;
        } catch (IllegalArgumentException e) {
            throw new MealCrawlingException("URL 생성 실패: " + pathOnly, e);
        }
    }
}