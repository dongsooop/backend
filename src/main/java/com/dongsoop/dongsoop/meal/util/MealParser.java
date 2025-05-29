package com.dongsoop.dongsoop.meal.util;

import com.dongsoop.dongsoop.meal.entity.MealDetails;
import com.dongsoop.dongsoop.meal.entity.MealType;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Component
@Slf4j
public class MealParser {

    private static final Pattern DATE_RANGE_PATTERN =
            Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2})\\s+~\\s+(\\d{4}\\.\\d{2}\\.\\d{2})");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final int MAX_MENU_LENGTH = 800;
    private static final int MAX_DAYS = 5;
    private static final String DEFAULT_EMPTY_MENU = "식단 정보 없음";

    private static final Function<String, String> CLEAN_MENU_TEXT = text ->
            text.replaceAll("<br\\s*/?>", " ")
                    .replaceAll("\\[점심\\]\\s*", "")
                    .replaceAll("\\s+", " ")
                    .trim();

    private static final Map<MealType, Function<Document, List<String>>> MENU_PARSE_STRATEGIES = Map.of(
            MealType.KOREAN, doc -> parseMenusByType(doc, "한식"),
            MealType.SPECIAL, doc -> parseMenusByType(doc, "별미")
    );

    private static List<String> parseMenusByType(Document document, String menuType) {
        log.debug("🔍 {} 메뉴 파싱 시작", menuType);

        Optional<Elements> menuRowsOpt = findMenuRows(document, menuType);

        if (menuRowsOpt.isEmpty()) {
            log.warn("⚠️ {} 메뉴 행을 찾을 수 없습니다.", menuType);
            return Collections.nCopies(MAX_DAYS, DEFAULT_EMPTY_MENU);
        }

        Elements menuRows = menuRowsOpt.get();
        Element firstRow = menuRows.first();
        Elements cells = firstRow.select("td");

        log.debug("📋 {} 메뉴 - 발견된 셀 개수: {}", menuType, cells.size());

        List<String> result = extractMenusFromCells(cells);
        log.debug("🍽️ {} 메뉴 파싱 결과: {}", menuType, result);

        return result;
    }

    private static Optional<Elements> findMenuRows(Document document, String menuType) {
        return document.select("tr:contains(" + menuType + " 메뉴)").stream()
                .map(Element::nextElementSibling)
                .filter(Objects::nonNull)
                .filter(row -> "tr".equals(row.tagName()))
                .filter(row -> row.select("td").size() >= 3)
                .findFirst()
                .map(row -> {
                    Elements result = new Elements();
                    result.add(row);
                    return result;
                });
    }

    private static List<String> extractMenusFromCells(Elements menuCells) {
        int cellCount = Math.min(menuCells.size(), MAX_DAYS);

        log.debug("📊 셀 정보 - 총 셀 개수: {}, 처리할 셀 개수: {}", menuCells.size(), cellCount);

        return IntStream.range(0, MAX_DAYS)
                .mapToObj(i -> extractMenuFromCell(menuCells, cellCount, i))
                .collect(Collectors.toList());
    }

    private static String extractMenuFromCell(Elements menuCells, int cellCount, int index) {
        if (index >= cellCount) {
            log.debug("🚫 인덱스 {} >= 셀 개수 {}, 기본 메뉴 반환", index, cellCount);
            return DEFAULT_EMPTY_MENU;
        }

        Element cell = menuCells.get(index);
        String rawHtml = cell.html();
        String cleanedText = CLEAN_MENU_TEXT.apply(rawHtml);
        String normalizedMenu = normalizeMenu(cleanedText);
        String finalMenu = truncateText(normalizedMenu, MAX_MENU_LENGTH);

        log.debug("🍽️ [{}일차] 원본HTML: {}", index + 1, rawHtml.replaceAll("\\s+", " ").trim());
        log.debug("🧹 [{}일차] 정제텍스트: '{}'", index + 1, cleanedText);
        log.debug("📝 [{}일차] 정규화메뉴: '{}'", index + 1, normalizedMenu);
        log.debug("✅ [{}일차] 최종메뉴: '{}'", index + 1, finalMenu);

        return finalMenu;
    }

    private static String normalizeMenu(String menu) {
        String result = Optional.ofNullable(menu)
                .filter(m -> !m.isEmpty() && !"-".equals(m.trim()))
                .orElse(DEFAULT_EMPTY_MENU);

        if (DEFAULT_EMPTY_MENU.equals(result) && menu != null) {
            log.debug("🚫 메뉴 정규화에서 필터링됨 - 원본: '{}', 길이: {}, trim: '{}'",
                    menu, menu.length(), menu.trim());
        }

        return result;
    }

    private static String truncateText(String text, int maxLength) {
        return Optional.ofNullable(text)
                .filter(t -> t.length() > maxLength)
                .map(t -> {
                    log.warn("텍스트가 {}자를 초과하여 잘라냅니다. 원본 길이: {}", maxLength, t.length());
                    return t.substring(0, maxLength - 3) + "...";
                })
                .orElse(text);
    }

    public List<MealDetails> parseWeeklyMeal(Document document) {
        log.info("HTML 구조 기반 식단 파싱 시작");

        CompletableFuture<DateRange> dateRangeFuture =
                CompletableFuture.supplyAsync(() -> parseDateRange(document));

        CompletableFuture<Map<MealType, List<String>>> menusFuture =
                CompletableFuture.supplyAsync(() -> parseAllMenus(document));

        return dateRangeFuture.thenCombine(menusFuture, this::buildMealDetailsList)
                .join();
    }

    private DateRange parseDateRange(Document document) {
        return Optional.of(document.text())
                .map(DATE_RANGE_PATTERN::matcher)
                .filter(Matcher::find)
                .map(this::extractDateRange)
                .filter(this::isValidDateRange)  // 유효한 날짜 범위인지 확인
                .orElseGet(this::getCurrentWeekDateRange);
    }

    private DateRange extractDateRange(Matcher matcher) {
        try {
            LocalDate startDate = LocalDate.parse(matcher.group(1), DATE_FORMATTER);
            LocalDate endDate = LocalDate.parse(matcher.group(2), DATE_FORMATTER);

            log.debug("📅 HTML에서 파싱된 날짜 범위: {} ~ {}", startDate, endDate);
            return new DateRange(startDate, endDate);
        } catch (Exception e) {
            log.warn("⚠️ 날짜 파싱 실패: {}, 현재 주로 대체", e.getMessage());
            return getCurrentWeekDateRange();
        }
    }

    private boolean isValidDateRange(DateRange dateRange) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();

        // 현재 연도와 1년 차이 이내의 날짜만 유효하다고 판단
        boolean isValid = Math.abs(dateRange.startDate().getYear() - currentYear) <= 1 &&
                Math.abs(dateRange.endDate().getYear() - currentYear) <= 1;

        if (!isValid) {
            log.warn("⚠️ 파싱된 날짜가 유효하지 않음: {} ~ {} (현재 연도: {})",
                    dateRange.startDate(), dateRange.endDate(), currentYear);
        }

        return isValid;
    }

    private DateRange getCurrentWeekDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);

        log.warn("날짜 범위를 파싱할 수 없어 현재 주로 설정: {} ~ {}", monday, monday.plusDays(4));
        return new DateRange(monday, monday.plusDays(4));
    }

    private Map<MealType, List<String>> parseAllMenus(Document document) {
        return MENU_PARSE_STRATEGIES.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().apply(document)
                ));
    }

    private List<MealDetails> buildMealDetailsList(DateRange dateRange, Map<MealType, List<String>> menuMap) {
        List<MealDetails> mealDetails = IntStream.range(0, MAX_DAYS)
                .boxed()
                .flatMap(dayIndex -> createDayMeals(dayIndex, dateRange, menuMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("총 {}개의 식단 데이터가 생성됩니다.", mealDetails.size());
        return mealDetails;
    }

    private Stream<MealDetails> createDayMeals(int dayIndex, DateRange dateRange,
                                               Map<MealType, List<String>> menuMap) {
        LocalDate currentDate = dateRange.startDate().plusDays(dayIndex);
        String dayName = DayOfWeekUtil.toKorean(currentDate.getDayOfWeek());

        return Arrays.stream(MealType.values())
                .map(type -> createMealDetails(currentDate, dayName, type,
                        menuMap.get(type).get(dayIndex)));
    }

    private MealDetails createMealDetails(LocalDate date, String dayName, MealType mealType, String menu) {
        return MealDetails.builder()
                .mealDate(date)
                .dayOfWeek(dayName)
                .mealType(mealType)
                .menuItems(menu)
                .build();
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}