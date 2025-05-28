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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Component
@Slf4j
public class MealParser {

    private static final List<String> DAY_NAMES = List.of("월", "화", "수", "목", "금");
    private static final Pattern DATE_RANGE_PATTERN =
            Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2})\\s+~\\s+(\\d{4}\\.\\d{2}\\.\\d{2})");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final int MAX_MENU_LENGTH = 800;
    private static final int MAX_DAYS = 5;

    private static final Predicate<String> IS_VALID_MENU = text ->
            !text.isEmpty() && !"-".equals(text);

    private static final Function<String, String> CLEAN_MENU_TEXT = text ->
            text.replaceAll("<br\\s*/?>", " ")
                    .replaceAll("\\[점심\\]\\s*", "")
                    .replaceAll("\\s+", " ")
                    .trim();

    private static final Map<MealType, BiFunction<Document, String, List<String>>> MENU_PARSE_STRATEGIES = Map.of(
            MealType.KOREAN, (doc, type) -> parseMenusByType(doc, "한식"),
            MealType.SPECIAL, (doc, type) -> parseMenusByType(doc, "별미")
    );

    private static List<String> parseMenusByType(Document document, String menuType) {
        return findMenuRows(document, menuType)
                .map(rows -> rows.first().select("td"))
                .map(MealParser::extractMenusFromCells)
                .orElseGet(() -> {
                    log.info("{} 메뉴 행을 찾을 수 없습니다.", menuType);
                    return Collections.nCopies(MAX_DAYS, "");
                });
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
                    return Optional.of(result);
                })
                .orElse(Optional.empty());
    }

    private static List<String> extractMenusFromCells(Elements menuCells) {
        int cellCount = Math.min(menuCells.size(), MAX_DAYS);

        return IntStream.range(0, MAX_DAYS)
                .mapToObj(i -> Optional.of(i)
                        .filter(index -> index < cellCount)
                        .map(menuCells::get)
                        .map(Element::html)
                        .map(CLEAN_MENU_TEXT)
                        .filter(IS_VALID_MENU)
                        .map(menu -> truncateText(menu, MAX_MENU_LENGTH))
                        .orElse("")
                )
                .collect(Collectors.toList());
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
                .orElseGet(this::getCurrentWeekDateRange);
    }

    private DateRange extractDateRange(Matcher matcher) {
        return new DateRange(
                LocalDate.parse(matcher.group(1), DATE_FORMATTER),
                LocalDate.parse(matcher.group(2), DATE_FORMATTER)
        );
    }

    private DateRange getCurrentWeekDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);

        log.warn("날짜 범위를 파싱할 수 없어 현재 주로 설정: {} ~ {}", monday, monday.plusDays(4));
        return new DateRange(monday, monday.plusDays(4));
    }

    private Map<MealType, List<String>> parseAllMenus(Document document) {
        return MENU_PARSE_STRATEGIES.entrySet().parallelStream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().apply(document, entry.getKey().getDescription())
                ));
    }

    private List<MealDetails> buildMealDetailsList(DateRange dateRange, Map<MealType, List<String>> menuMap) {
        List<MealDetails> mealDetails = IntStream.range(0, MAX_DAYS)
                .parallel()
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
        String dayName = DAY_NAMES.get(dayIndex);

        return Arrays.stream(MealType.values())
                .map(type -> Map.entry(type, menuMap.get(type).get(dayIndex)))
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> createMealDetails(currentDate, dayName, entry.getKey(), entry.getValue()));
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