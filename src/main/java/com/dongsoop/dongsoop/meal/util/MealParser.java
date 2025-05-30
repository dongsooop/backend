package com.dongsoop.dongsoop.meal.util;

import com.dongsoop.dongsoop.meal.entity.Meal;
import com.dongsoop.dongsoop.meal.entity.MealType;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class MealParser {

    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2})\\s+~\\s+(\\d{4}\\.\\d{2}\\.\\d{2})");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final int MAX_DAYS = 5;

    private final TextProcessingUtil textProcessingUtil;

    public List<Meal> parseWeeklyMeal(Document document) {
        DateRange dateRange = parseDateRange(document);
        Map<MealType, List<String>> menuMap = parseAllMenus(document);
        return buildMealList(dateRange, menuMap);
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
            return getCurrentWeekDateRange();
        }
    }

    private boolean validateDateRange(DateRange dateRange) {
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        return Math.abs(dateRange.startDate().getYear() - currentYear) <= 1 &&
                Math.abs(dateRange.endDate().getYear() - currentYear) <= 1;
    }

    private DateRange getCurrentWeekDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        return new DateRange(monday, monday.plusDays(4));
    }

    private Map<MealType, List<String>> parseAllMenus(Document document) {
        Map<MealType, List<String>> menuMap = new EnumMap<>(MealType.class);
        menuMap.put(MealType.KOREAN, parseMenusByType(document, "한식"));
        menuMap.put(MealType.SPECIAL, parseMenusByType(document, "별미"));
        return menuMap;
    }

    private List<String> parseMenusByType(Document document, String menuTypeName) {
        return findMenuRows(document, menuTypeName)
                .map(this::extractMenusFromFirstRow)
                .orElse(Collections.nCopies(MAX_DAYS, textProcessingUtil.getDefaultEmptyMenu()));
    }

    private Optional<Element> findMenuRows(Document document, String menuType) {
        return document.select("tr:contains(" + menuType + " 메뉴)").stream()
                .map(Element::nextElementSibling)
                .filter(Objects::nonNull)
                .filter(row -> "tr".equals(row.tagName()))
                .filter(row -> row.select("td").size() >= 3)
                .findFirst();
    }

    private List<String> extractMenusFromFirstRow(Element menuRow) {
        Elements cells = menuRow.select("td");
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