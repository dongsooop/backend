package com.dongsoop.dongsoop.meal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TextProcessingUtilTest {

    private static final int MAX_MENU_LENGTH = 800;

    private final TextProcessingUtil textProcessingUtil = new TextProcessingUtil();

    @Test
    @DisplayName("태그와 HTML 엔티티가 섞인 메뉴는 평문으로 돌아온다")
    void returnsPlainTextForMarkupAndEntities() {
        String result = textProcessingUtil.processMenuText("돈까스<br>김치&amp;단무지<br/>&lt;국&gt;");

        assertEquals("돈까스 김치&단무지 <국>", result);
    }

    @Test
    @DisplayName("따옴표 엔티티도 원래 문자로 돌아온다")
    void returnsPlainTextForQuoteEntities() {
        String result = textProcessingUtil.processMenuText("&quot;오늘의 메뉴&quot; &#39;특선&#39;");

        assertEquals("\"오늘의 메뉴\" '특선'", result);
    }

    @Test
    @DisplayName("점심 표기가 붙은 메뉴는 그 표기 없이 돌아온다")
    void returnsMenuWithoutLunchPrefix() {
        String result = textProcessingUtil.processMenuText("[점심] 제육볶음");

        assertEquals("제육볶음", result);
    }

    @Test
    @DisplayName("공백이 흩어진 메뉴는 단어 사이 공백 하나로 정리되어 돌아온다")
    void returnsMenuWithNormalizedWhitespace() {
        String result = textProcessingUtil.processMenuText("  밥   국\t\t김치  ");

        assertEquals("밥 국 김치", result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-", "  -  ", "<br>"})
    @DisplayName("내용이 없는 메뉴는 기본 문구로 돌아온다")
    void returnsDefaultMenuWhenEmpty(String rawMenu) {
        String result = textProcessingUtil.processMenuText(rawMenu);

        assertEquals(textProcessingUtil.getDefaultEmptyMenu(), result);
    }

    @Test
    @DisplayName("상한을 넘는 메뉴는 상한 이하로 잘려서 돌아온다")
    void returnsTruncatedMenuWhenTooLong() {
        String result = textProcessingUtil.processMenuText("가".repeat(MAX_MENU_LENGTH + 1));

        assertTrue(result.length() <= MAX_MENU_LENGTH);
        assertTrue(result.endsWith("..."));
    }

    @Test
    @DisplayName("상한 길이의 메뉴는 잘리지 않고 그대로 돌아온다")
    void returnsMenuAsIsAtMaxLength() {
        String rawMenu = "가".repeat(MAX_MENU_LENGTH);

        String result = textProcessingUtil.processMenuText(rawMenu);

        assertEquals(rawMenu, result);
    }
}
