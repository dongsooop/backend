package com.dongsoop.dongsoop.meal.util;

import com.dongsoop.dongsoop.meal.entity.Meal;
import com.dongsoop.dongsoop.meal.entity.MealType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class MealParser {

    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2})\\s+~\\s+(\\d{4}\\.\\d{2}\\.\\d{2})");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final int MAX_DAYS = 5;

    private static final Map<MealType, String> MENU_TYPE_NAMES = Map.of(
            MealType.KOREAN, "한식",
            MealType.SPECIAL, "별미"
    );

    private static final Predicate<DateRange> IS_VALID_DATE_RANGE = dateRange -> {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        return Math.abs(dateRange.startDate().getYear() - currentYear) <= 1 &&
                Math.abs(dateRange.endDate().getYear() - currentYear) <= 1;
    };

    private final TextProcessingUtil textProcessingUtil;

    public List<Meal> parseWeeklyMeal(Document document) {
        log.info("HTML 구조 기반 식단 파싱 시작");

        DateRange dateRange = parseDateRange(document);
        Map<MealType, List<String>> menuMap = parseAllMenus(document);
        List<Meal> result = buildMealList(dateRange, menuMap);

        log.info("총 {}개의 식단 데이터가 생성되었습니다.", result.size());
        return result;
    }

    private DateRange parseDateRange(Document document) {
        return Optional.of(document.text())
                .map(DATE_RANGE_PATTERN::matcher)
                .filter(Matcher::find)
                .map(this::extractDateRange)
                .filter(this::validateDateRange)
                .orElseGet(this::getCurrentWeekDateRange);
    }

    private DateRange extractDateRange(Matcher matcher) {
        try {
            LocalDate startDate = LocalDate.parse(matcher.group(1), DATE_FORMATTER);
            LocalDate endDate = LocalDate.parse(matcher.group(2), DATE_FORMATTER);
            return new DateRange(startDate, endDate);
        } catch (Exception e) {
            log.warn("⚠️ 날짜 파싱 실패: {}, 현재 주로 대체", e.getMessage());
            return getCurrentWeekDateRange();
        }
    }

    private boolean validateDateRange(DateRange dateRange) {
        boolean isValid = IS_VALID_DATE_RANGE.test(dateRange);

        Optional.of(dateRange)
                .filter(range -> !isValid)
                .ifPresent(range -> {
                    int currentYear = LocalDate.now().getYear();
                    log.warn("⚠️ 파싱된 날짜가 유효하지 않음: {} ~ {} (현재 연도: {})",
                            range.startDate(), range.endDate(), currentYear);
                });

        return isValid;
    }

    private DateRange getCurrentWeekDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);

        log.warn("날짜 범위를 파싱할 수 없어 현재 주로 설정: {} ~ {}", monday, monday.plusDays(4));
        return new DateRange(monday, monday.plusDays(4));
    }

    private Map<MealType, List<String>> parseAllMenus(Document document) {
        return MENU_TYPE_NAMES.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> parseMenusByType(document, entry.getValue())
                ));
    }

    private List<String> parseMenusByType(Document document, String menuTypeName) {
        return findMenuRows(document, menuTypeName)
                .map(this::extractMenusFromFirstRow)
                .orElseGet(() -> {
                    log.warn("⚠️ {} 메뉴 행을 찾을 수 없습니다.", menuTypeName);
                    return Collections.nCopies(MAX_DAYS, textProcessingUtil.getDefaultEmptyMenu());
                });
    }

    private Optional<Elements> findMenuRows(Document document, String menuType) {
        return document.select("tr:contains(" + menuType + " 메뉴)").stream()
                .map(Element::nextElementSibling)
                .filter(Objects::nonNull)
                .filter(row -> "tr".equals(row.tagName()))
                .filter(row -> row.select("td").size() >= 3)
                .findFirst()
                .map(this::wrapInElements);
    }

    private Elements wrapInElements(Element row) {
        Elements result = new Elements();
        result.add(row);
        return result;
    }

    private List<String> extractMenusFromFirstRow(Elements menuRows) {
        Element firstRow = menuRows.first();
        Elements cells = firstRow.select("td");
        return extractMenusFromCells(cells);
    }

    private List<String> extractMenusFromCells(Elements menuCells) {
        int cellCount = Math.min(menuCells.size(), MAX_DAYS);

        return IntStream.range(0, MAX_DAYS)
                .mapToObj(index -> extractMenuFromCell(menuCells, cellCount, index))
                .collect(Collectors.toList());
    }

    private String extractMenuFromCell(Elements menuCells, int cellCount, int index) {
        return Optional.of(index)
                .filter(i -> i < cellCount)
                .map(i -> menuCells.get(i))
                .map(Element::html)
                .map(textProcessingUtil::processMenuText)
                .orElse(textProcessingUtil.getDefaultEmptyMenu());
    }

    private List<Meal> buildMealList(DateRange dateRange, Map<MealType, List<String>> menuMap) {
        return IntStream.range(0, MAX_DAYS)
                .boxed()
                .map(dayIndex -> createDayMeals(dayIndex, dateRange, menuMap))
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Meal> createDayMeals(int dayIndex, DateRange dateRange, Map<MealType, List<String>> menuMap) {
        LocalDate currentDate = dateRange.startDate().plusDays(dayIndex);
        String dayName = DayOfWeekUtil.toKorean(currentDate.getDayOfWeek());

        return Arrays.stream(MealType.values())
                .map(type -> createMeal(currentDate, dayName, type, menuMap.get(type).get(dayIndex)))
                .collect(Collectors.toList());
    }

    private Meal createMeal(LocalDate date, String dayName, MealType mealType, String menu) {
        return Meal.builder()
                .mealDate(date)
                .dayOfWeek(dayName)
                .mealType(mealType)
                .menuItems(menu)
                .build();
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}