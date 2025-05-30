package com.dongsoop.dongsoop.meal.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
@Slf4j
public class TextProcessingUtil {

    private static final int MAX_MENU_LENGTH = 800;
    private static final String DEFAULT_EMPTY_MENU = "식단 정보 없음";

    private static final Function<String, String> CLEAN_MENU_TEXT = text ->
            text.replaceAll("<br\\s*/?>", " ")
                    .replaceAll("\\[점심\\]\\s*", "")
                    .replaceAll("\\s+", " ")
                    .trim();

    private static final Function<String, String> DECODE_HTML_ENTITIES = text ->
            text.replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'");

    private static final Predicate<String> IS_EMPTY_MENU = menu ->
            Objects.isNull(menu) || menu.isEmpty() || "-".equals(menu.trim());

    private static final Function<String, String> NORMALIZE_MENU = menu ->
            Optional.ofNullable(menu)
                    .filter(IS_EMPTY_MENU.negate())
                    .orElse(DEFAULT_EMPTY_MENU);

    public String processMenuText(String rawHtml) {
        String cleanedText = CLEAN_MENU_TEXT.apply(rawHtml);
        String decodedText = DECODE_HTML_ENTITIES.apply(cleanedText);
        String normalizedMenu = NORMALIZE_MENU.apply(decodedText);

        return truncateText(normalizedMenu);
    }

    private String truncateText(String text) {
        return Optional.ofNullable(text)
                .filter(t -> t.length() > MAX_MENU_LENGTH)
                .map(t -> {
                    log.warn("텍스트가 {}자를 초과하여 잘라냅니다. 원본 길이: {}", MAX_MENU_LENGTH, t.length());
                    return t.substring(0, MAX_MENU_LENGTH - 3) + "...";
                })
                .orElse(text);
    }

    public String getDefaultEmptyMenu() {
        return DEFAULT_EMPTY_MENU;
    }
}