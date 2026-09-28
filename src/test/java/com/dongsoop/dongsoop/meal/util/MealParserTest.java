package com.dongsoop.dongsoop.meal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dongsoop.dongsoop.meal.entity.Meal;
import com.dongsoop.dongsoop.meal.entity.MealType;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MealParserTest {

    private static final int DAYS = 5;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 13);
    private static final LocalDate CURRENT_MONDAY = LocalDate.of(2026, 5, 11);
    private static final DateTimeFormatter PAGE_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final TextProcessingUtil textProcessingUtil = new TextProcessingUtil();
    private final Clock clock = Clock.fixed(TODAY.atStartOfDay(KST).toInstant(), KST);
    private final MealParser mealParser = new MealParser(textProcessingUtil, clock);

    @Test
    @DisplayName("주간 날짜 범위가 적힌 페이지는 그 범위의 5일치를 돌려준다")
    void usesDateRangeFromPage() {
        LocalDate start = LocalDate.of(2026, 9, 14);
        Document document = page(dateRangeText(start, start.plusDays(4)), menuTable());

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        assertEquals(expectedDates(start), datesOf(meals));
    }

    @Test
    @DisplayName("날짜 범위가 없는 페이지는 이번 주 월요일부터의 5일치를 돌려준다")
    void fallsBackToCurrentWeekWhenDateRangeMissing() {
        Document document = page("식단 안내", menuTable());

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        assertEquals(expectedDates(CURRENT_MONDAY), datesOf(meals));
    }

    @Test
    @DisplayName("올해와 동떨어진 연도의 날짜 범위는 무시하고 이번 주 5일치를 돌려준다")
    void fallsBackToCurrentWeekWhenYearIsTooFarAway() {
        LocalDate staleStart = LocalDate.of(2019, 3, 4);
        Document document = page(dateRangeText(staleStart, staleStart.plusDays(4)), menuTable());

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        assertEquals(expectedDates(CURRENT_MONDAY), datesOf(meals));
    }

    @Test
    @DisplayName("형식만 날짜이고 실재하지 않는 날짜는 무시하고 이번 주 5일치를 돌려준다")
    void fallsBackToCurrentWeekWhenDateDoesNotExist() {
        Document document = page("2026.13.45 ~ 2026.13.49", menuTable());

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        assertEquals(expectedDates(CURRENT_MONDAY), datesOf(meals));
    }

    @Test
    @DisplayName("5일치가 채워진 메뉴 표는 날마다 해당 메뉴를 돌려준다")
    void returnsMenuOfEachDay() {
        Document document = page("식단 안내", menuTable());

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        assertEquals(List.of("한식1", "한식2", "한식3", "한식4", "한식5"), menusOf(meals, MealType.KOREAN));
    }

    @Test
    @DisplayName("메뉴 표가 없는 타입은 5일 모두 기본 문구를 돌려준다")
    void returnsDefaultMenuWhenTableMissing() {
        Document document = page("식단 안내", menuRows("한식", "한식1", "한식2", "한식3", "한식4", "한식5"));

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        assertEquals(defaultMenus(DAYS), menusOf(meals, MealType.SPECIAL));
    }

    @Test
    @DisplayName("메뉴 표가 5일보다 적으면 빈 날은 기본 문구를 돌려준다")
    void fillsRemainingDaysWithDefaultMenu() {
        Document document = page("식단 안내", menuRows("한식", "한식1", "한식2", "한식3"));

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        String empty = textProcessingUtil.getDefaultEmptyMenu();
        assertEquals(List.of("한식1", "한식2", "한식3", empty, empty), menusOf(meals, MealType.KOREAN));
    }

    @Test
    @DisplayName("메뉴 표가 5일보다 많으면 앞의 5일치만 돌려준다")
    void ignoresMenusBeyondFiveDays() {
        Document document = page("식단 안내",
                menuRows("한식", "한식1", "한식2", "한식3", "한식4", "한식5", "한식6", "한식7"));

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        assertEquals(List.of("한식1", "한식2", "한식3", "한식4", "한식5"), menusOf(meals, MealType.KOREAN));
    }

    @Test
    @DisplayName("메뉴 표 자리에 표가 아닌 내용이 있으면 5일 모두 기본 문구를 돌려준다")
    void returnsDefaultMenuWhenRowIsNotTable() {
        Document document = page("식단 안내", "<table><tr><td>한식 메뉴</td></tr></table><div>준비 중</div>");

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        assertEquals(defaultMenus(DAYS), menusOf(meals, MealType.KOREAN));
    }

    @Test
    @DisplayName("메뉴 칸이 세 개 미만인 표는 5일 모두 기본 문구를 돌려준다")
    void returnsDefaultMenuWhenRowHasTooFewCells() {
        Document document = page("식단 안내", menuRows("한식", "한식1", "한식2"));

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        assertEquals(defaultMenus(DAYS), menusOf(meals, MealType.KOREAN));
    }

    @Test
    @DisplayName("결과는 5일치를 타입마다 담고 날짜에 맞는 한글 요일을 붙여 돌려준다")
    void returnsFiveDaysForEachMealType() {
        Document document = page("식단 안내", menuTable());

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        assertEquals(DAYS * MealType.values().length, meals.size());
        assertEquals(List.of("월", "화", "수", "목", "금"),
                meals.stream()
                        .filter(meal -> meal.getMealType() == MealType.KOREAN)
                        .map(Meal::getDayOfWeek)
                        .toList());
    }

    @Test
    @DisplayName("\"단품 메뉴\" 라벨 행을 SPECIAL 메뉴로 읽는다")
    void readsSingleMenuLabelAsSpecial() {
        Document document = page("식단 안내",
                menuRows("한식", "한식1", "한식2", "한식3", "한식4", "한식5")
                        + menuRows("단품", "단품1", "단품2", "단품3", "단품4", "단품5"));

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        assertEquals(List.of("단품1", "단품2", "단품3", "단품4", "단품5"), menusOf(meals, MealType.SPECIAL));
    }

    @Test
    @DisplayName("단품과 별미 행이 함께 있으면 단품 행을 우선한다")
    void prefersSingleMenuOverSpecialLabel() {
        Document document = page("식단 안내",
                menuRows("별미", "별미1", "별미2", "별미3", "별미4", "별미5")
                        + menuRows("단품", "단품1", "단품2", "단품3", "단품4", "단품5"));

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        assertEquals(List.of("단품1", "단품2", "단품3", "단품4", "단품5"), menusOf(meals, MealType.SPECIAL));
    }

    @Test
    @DisplayName("실제 페이지 구조(라벨 행 + 내용 행, 빈 칸은 -)를 그대로 읽는다")
    void parsesRealPageStructure() {
        Document document = Jsoup.parse(realPage("<span>15:00까지만 운영합니다.</span>"));

        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        String empty = textProcessingUtil.getDefaultEmptyMenu();
        assertEquals(List.of("백미밥, 달걀국", "백미밥, 육개장", "추석 휴무", empty, empty),
                menusOf(meals, MealType.KOREAN));
        assertEquals(List.of("덮밥: 스팸김치볶음밥 라면류, 돈까스류", empty, "덮밥: 삼겹살덮밥", empty, empty),
                menusOf(meals, MealType.SPECIAL));
    }

    @Test
    @DisplayName("공지사항 행의 문장을 돌려주고 여러 줄이면 줄바꿈으로 잇는다")
    void parsesNoticeLines() {
        Document document = Jsoup.parse(realPage("<span>15:00까지만 운영합니다.</span><span>라스트 오더는 14:00입니다.</span>"));

        Optional<String> notice = mealParser.parseNotice(document);

        assertEquals(Optional.of("15:00까지만 운영합니다.\n라스트 오더는 14:00입니다."), notice);
    }

    @Test
    @DisplayName("공지사항 칸이 비어 있거나 - 면 공지 없음으로 돌려준다")
    void returnsEmptyNoticeWhenCellIsBlankOrDash() {
        assertEquals(Optional.empty(), mealParser.parseNotice(Jsoup.parse(realPage(" - "))));
        assertEquals(Optional.empty(), mealParser.parseNotice(Jsoup.parse(realPage(""))));
    }

    @Test
    @DisplayName("공지사항 행이 없는 페이지는 공지 없음으로 돌려준다")
    void returnsEmptyNoticeWhenRowMissing() {
        Document document = page("식단 안내", menuTable());

        assertEquals(Optional.empty(), mealParser.parseNotice(document));
    }


    // 2026-09-22 학교 페이지의 표 구조를 줄인 것
    private String realPage(String noticeCellHtml) {
        return "<html><body><div class=\"table_1\"><table>"
                + "<caption>일주일간의 식단을 요일별로 한식 메뉴, 별미 메뉴, 공지사항별로 안내합니다.</caption>"
                + "<thead><tr><th>월<span>(2026.09.21)</span></th><th>화<span>(2026.09.22)</span></th>"
                + "<th>수<span>(2026.09.23)</span></th><th>목<span>(2026.09.24)</span></th><th>금<span>(2026.09.25)</span></th></tr></thead>"
                + "<tbody>"
                + "<tr><td class=\"tit\" colspan=\"5\">한식 메뉴</td></tr>"
                + "<tr><td>[점심]<br/>백미밥, 달걀국<br/><br/></td><td class=\"highlight\">[점심]<br/>백미밥, 육개장<br/><br/></td>"
                + "<td>[점심]<br/>추석 휴무<br/><br/></td><td>-</td><td>-</td></tr>"
                + "<tr><td class=\"tit\" colspan=\"5\">단품 메뉴</td></tr>"
                + "<tr><td>[점심]<br/>덮밥: 스팸김치볶음밥\n<br/>라면류, 돈까스류<br/><br/></td><td class=\"highlight\">-</td>"
                + "<td>[점심]<br/>덮밥: 삼겹살덮밥<br/><br/></td><td>-</td><td>-</td></tr>"
                + "<tr><td class=\"tit\" colspan=\"5\">공지사항</td></tr>"
                + "<tr><td class=\"notice-line\" colspan=\"5\">" + noticeCellHtml + "</td></tr>"
                + "</tbody></table></div></body></html>";
    }

    private String dateRangeText(LocalDate start, LocalDate end) {
        return start.format(PAGE_DATE) + " ~ " + end.format(PAGE_DATE);
    }

    private List<LocalDate> expectedDates(LocalDate start) {
        return IntStream.range(0, DAYS)
                .mapToObj(start::plusDays)
                .toList();
    }

    private List<LocalDate> datesOf(List<Meal> meals) {
        return meals.stream()
                .filter(meal -> meal.getMealType() == MealType.KOREAN)
                .map(Meal::getMealDate)
                .toList();
    }

    private List<String> menusOf(List<Meal> meals, MealType mealType) {
        return meals.stream()
                .filter(meal -> meal.getMealType() == mealType)
                .map(Meal::getMenuItems)
                .toList();
    }

    private List<String> defaultMenus(int size) {
        return IntStream.range(0, size)
                .mapToObj(index -> textProcessingUtil.getDefaultEmptyMenu())
                .toList();
    }

    private Document page(String header, String tableHtml) {
        return Jsoup.parse("<html><body><div>" + header + "</div>" + tableHtml + "</body></html>");
    }

    private String menuTable() {
        return menuRows("한식", "한식1", "한식2", "한식3", "한식4", "한식5")
                + menuRows("별미", "별미1", "별미2", "별미3", "별미4", "별미5");
    }

    private String menuRows(String menuTypeName, String... menus) {
        String cells = java.util.Arrays.stream(menus)
                .map(menu -> "<td>" + menu + "</td>")
                .collect(Collectors.joining());
        return "<table><tr><td>" + menuTypeName + " 메뉴</td></tr><tr>" + cells + "</tr></table>";
    }
}
