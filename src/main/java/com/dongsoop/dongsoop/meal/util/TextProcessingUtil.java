package com.dongsoop.dongsoop.meal.util;

import java.util.Optional;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class TextProcessingUtil {

    private static final int MAX_MENU_LENGTH = 800;
    private static final String DEFAULT_EMPTY_MENU = "식단 정보 없음";

    public String processMenuText(String rawHtml) {
        String cleanedText = cleanMenuText(rawHtml);
        String decodedText = decodeHtmlEntities(cleanedText);
        String normalizedMenu = normalizeMenu(decodedText);
        return truncateText(normalizedMenu);
    }

    private String cleanMenuText(String text) {
        return text.replaceAll("<br\\s*/?>", " ")
                .replaceAll("\\[점심\\]\\s*", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String decodeHtmlEntities(String text) {
        return text.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private String normalizeMenu(String menu) {
        return Optional.ofNullable(menu)
                .filter(m -> !isEmptyMenu(m))
                .orElse(DEFAULT_EMPTY_MENU);
    }

    private boolean isEmptyMenu(String menu) {
        return Objects.isNull(menu) || menu.isEmpty() || "-".equals(menu.trim());
    }

    private String truncateText(String text) {
        return Optional.ofNullable(text)
                .filter(t -> t.length() > MAX_MENU_LENGTH)
                .map(t -> t.substring(0, MAX_MENU_LENGTH - 3) + "...")
                .orElse(text);
    }

    public String getDefaultEmptyMenu() {
        return DEFAULT_EMPTY_MENU;
    }
}