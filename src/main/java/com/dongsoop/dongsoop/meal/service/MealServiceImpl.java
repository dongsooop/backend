package com.dongsoop.dongsoop.meal.service;

import com.dongsoop.dongsoop.exception.domain.meal.MealCrawlingException;
import com.dongsoop.dongsoop.exception.domain.meal.MealNotFoundException;
import com.dongsoop.dongsoop.meal.dto.MealDailyResponse;
import com.dongsoop.dongsoop.meal.dto.MealListDto;
import com.dongsoop.dongsoop.meal.dto.MealWeeklyResponse;
import com.dongsoop.dongsoop.meal.entity.Meal;
import com.dongsoop.dongsoop.meal.entity.MealType;
import com.dongsoop.dongsoop.meal.repository.MealRepository;
import com.dongsoop.dongsoop.meal.util.DayOfWeekUtil;
import com.dongsoop.dongsoop.meal.util.MealParser;
import com.dongsoop.dongsoop.meal.util.UrlEncodingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealServiceImpl implements MealService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_EMPTY_MENU = "식단 정보 없음";

    private static final Map<Boolean, Consumer<Integer>> CLEANUP_LOG_ACTIONS = Map.of(
            true, count -> log.info("✅ 처리 완료: {} 건", count),
            false, count -> log.info("ℹ️ 처리할 데이터가 없습니다.")
    );

    private static final Map<Boolean, Consumer<Integer>> CRAWLING_LOG_ACTIONS = Map.of(
            true, count -> log.info("✅ 처리 완료: {} 건", count),
            false, count -> log.info("ℹ️ 처리할 데이터가 없습니다.")
    );

    private final Map<Boolean, Function<CrawlingData, List<Meal>>> MEAL_SELECTION_STRATEGIES = Map.of(
            true, data -> data.allMeals(),
            false, this::filterNewMeals
    );

    private final MealRepository mealRepository;
    private final Map<String, Consumer<WeekDataContext>> DELETE_WEEK_ACTIONS = Map.of(
            "current", this::deleteCurrentWeekData,
            "next", this::deleteNextWeekData
    );
    private final MealParser mealParser;
    private final UrlEncodingUtil urlEncodingUtil;

    @Value("${meal.crawler.base-url}")
    private String mealBaseUrl;

    @Value("${meal.crawler.timeout:15000}")
    private int connectionTimeout;

    @Value("${meal.crawler.user-agent}")
    private String userAgent;

    @Override
    @Transactional(readOnly = true)
    public MealWeeklyResponse getCurrentWeekMealResponse() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.with(DayOfWeek.MONDAY);
        LocalDate endDate = today.with(DayOfWeek.FRIDAY);

        List<MealListDto> meals = mealRepository.findMealsByDateRangeList(startDate, endDate);

        return Optional.of(meals)
                .filter(list -> !list.isEmpty())
                .map(list -> buildWeeklyResponse(startDate, endDate, list))
                .orElseThrow(() -> new MealNotFoundException(startDate, endDate));
    }

    @Scheduled(cron = "0 30 22 * * FRI", zone = "Asia/Seoul")
    @Transactional
    public void crawlWeeklyMeal() {
        executeTask("자동 식단 크롤링", this::performCrawling);
    }

    @Scheduled(cron = "0 0 2 * * SUN", zone = "Asia/Seoul")
    @Transactional
    public void cleanupOldMealData() {
        executeTask("자동 식단 데이터 정리", this::performCleanup);
    }

    private void executeTask(String taskName, Runnable task) {
        log.info("=== {} 시작 === [{}]", taskName, LocalDateTime.now().format(DATE_TIME_FORMATTER));

        try {
            task.run();
        } catch (Exception e) {
            log.error("❌ {} 중 오류 발생", taskName, e);
        }

        log.info("=== {} 종료 === [{}]", taskName, LocalDateTime.now().format(DATE_TIME_FORMATTER));
    }

    private void performCrawling() {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);

        List<Meal> currentWeekMeals = crawlCurrentWeek(monday);
        List<Meal> nextWeekMeals = crawlNextWeek(monday);

        List<Meal> allMeals = new ArrayList<>();
        allMeals.addAll(currentWeekMeals);
        allMeals.addAll(nextWeekMeals);

        log.info("🌐 크롤링 완료 - 현재주: {}개, 다음주: {}개", currentWeekMeals.size(), nextWeekMeals.size());

        boolean isFirstCrawling = mealRepository.count() == 0;
        LocalDate lastDate = mealRepository.findMaxMealDate()
                .orElse(LocalDate.now().minusWeeks(1));
        LocalDate currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        CrawlingData crawlingData = new CrawlingData(allMeals, lastDate, currentWeekStart);
        List<Meal> newMeals = MEAL_SELECTION_STRATEGIES.get(isFirstCrawling).apply(crawlingData);

        log.info("📊 크롤링 결과 - 전체: {}개, 새로운 데이터: {}개", allMeals.size(), newMeals.size());

        Optional.of(newMeals)
                .filter(meals -> !meals.isEmpty())
                .ifPresent(this::saveMealData);

        CRAWLING_LOG_ACTIONS.get(!newMeals.isEmpty()).accept(newMeals.size());
    }

    private List<Meal> crawlCurrentWeek(LocalDate monday) {
        LocalDate nextMonday = monday.plusWeeks(1);
        String url = urlEncodingUtil.buildWeekUrl(mealBaseUrl, nextMonday, "pre");

        Document document = fetchDocument(url);
        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        logCrawlResult("current", meals);
        return meals;
    }

    private List<Meal> crawlNextWeek(LocalDate monday) {
        String url = urlEncodingUtil.buildWeekUrl(mealBaseUrl, monday, "next");

        Document document = fetchDocument(url);
        List<Meal> meals = mealParser.parseWeeklyMeal(document);

        List<Meal> processedMeals = Optional.of(meals)
                .filter(mealList -> isCurrentWeekData(mealList, monday))
                .map(mealList -> {
                    log.warn("⚠️ 다음주 크롤링인데 현재주 데이터 반환됨 - 날짜를 다음주로 강제 수정");
                    return adjustToNextWeek(mealList, monday.plusWeeks(1));
                })
                .orElse(meals);

        logCrawlResult("next", processedMeals);
        return processedMeals;
    }

    private boolean isCurrentWeekData(List<Meal> meals, LocalDate monday) {
        LocalDate currentWeekEnd = monday.plusDays(4);

        Predicate<Meal> isInCurrentWeek = meal ->
                !meal.getMealDate().isBefore(monday) && !meal.getMealDate().isAfter(currentWeekEnd);

        return meals.stream().allMatch(isInCurrentWeek);
    }

    private List<Meal> adjustToNextWeek(List<Meal> meals, LocalDate nextWeekStart) {
        return meals.stream()
                .map(meal -> createAdjustedMeal(meal, nextWeekStart))
                .collect(Collectors.toList());
    }

    private Meal createAdjustedMeal(Meal meal, LocalDate nextWeekStart) {
        int dayOffset = meal.getMealDate().getDayOfWeek().getValue() - 1;
        LocalDate nextWeekDate = nextWeekStart.plusDays(dayOffset);

        return Meal.builder()
                .mealDate(nextWeekDate)
                .dayOfWeek(meal.getDayOfWeek())
                .mealType(meal.getMealType())
                .menuItems(meal.getMenuItems())
                .build();
    }

    private void logCrawlResult(String weekType, List<Meal> meals) {
        Optional<LocalDate> minDate = meals.stream().map(Meal::getMealDate).min(LocalDate::compareTo);
        Optional<LocalDate> maxDate = meals.stream().map(Meal::getMealDate).max(LocalDate::compareTo);

        log.info("🗓️ {} 크롤링 결과: {} ~ {} ({}개)",
                weekType, minDate.orElse(null), maxDate.orElse(null), meals.size());
    }

    private List<Meal> filterNewMeals(CrawlingData data) {
        Predicate<Meal> isCurrentWeek = meal -> !meal.getMealDate().isBefore(data.currentWeekStart());
        Predicate<Meal> isAfterLastDate = meal -> meal.getMealDate().isAfter(data.lastDate());
        Predicate<Meal> shouldInclude = isCurrentWeek.or(isAfterLastDate);

        return data.allMeals().stream()
                .filter(shouldInclude)
                .collect(Collectors.toList());
    }

    private void performCleanup() {
        LocalDate cutoffDate = LocalDate.now().minusWeeks(2);
        int deletedCount = mealRepository.deleteOldMealData(cutoffDate);

        CLEANUP_LOG_ACTIONS.get(deletedCount > 0).accept(deletedCount);
    }

    private Document fetchDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .timeout(connectionTimeout)
                    .userAgent(userAgent)
                    .headers(Map.of(
                            "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                            "Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.5,en;q=0.3"
                    ))
                    .get();
        } catch (IOException e) {
            throw new MealCrawlingException(url, e);
        }
    }

    private void saveMealData(List<Meal> newMeals) {
        List<Meal> uniqueMeals = removeDuplicates(newMeals);
        deleteExistingData(uniqueMeals);
        saveToDatabase(uniqueMeals);
    }

    private List<Meal> removeDuplicates(List<Meal> meals) {
        Map<String, Meal> uniqueMap = new LinkedHashMap<>();

        for (Meal meal : meals) {
            String key = meal.getMealDate() + "_" + meal.getMealType();
            uniqueMap.putIfAbsent(key, meal);
        }

        return new ArrayList<>(uniqueMap.values());
    }

    private void deleteExistingData(List<Meal> meals) {
        LocalDate now = LocalDate.now();
        LocalDate currentWeekStart = now.with(DayOfWeek.MONDAY);
        LocalDate currentWeekEnd = now.with(DayOfWeek.FRIDAY);
        LocalDate nextWeekStart = currentWeekStart.plusWeeks(1);
        LocalDate nextWeekEnd = currentWeekEnd.plusWeeks(1);

        Predicate<Meal> isCurrentWeek = meal ->
                !meal.getMealDate().isBefore(currentWeekStart) && !meal.getMealDate().isAfter(currentWeekEnd);
        Predicate<Meal> isNextWeek = meal ->
                !meal.getMealDate().isBefore(nextWeekStart) && !meal.getMealDate().isAfter(nextWeekEnd);

        Optional.of(meals)
                .filter(mealList -> mealList.stream().anyMatch(isCurrentWeek))
                .ifPresent(mealList -> DELETE_WEEK_ACTIONS.get("current")
                        .accept(new WeekDataContext(currentWeekStart, currentWeekEnd, "현재 주")));

        Optional.of(meals)
                .filter(mealList -> mealList.stream().anyMatch(isNextWeek))
                .ifPresent(mealList -> DELETE_WEEK_ACTIONS.get("next")
                        .accept(new WeekDataContext(nextWeekStart, nextWeekEnd, "다음주")));
    }

    private void deleteCurrentWeekData(WeekDataContext context) {
        log.info("🔄 {}({} ~ {}) 기존 데이터 삭제", context.weekName(), context.startDate(), context.endDate());
        mealRepository.deleteByMealDateBetween(context.startDate(), context.endDate());
    }

    private void deleteNextWeekData(WeekDataContext context) {
        log.info("🔄 {}({} ~ {}) 기존 데이터 삭제", context.weekName(), context.startDate(), context.endDate());
        mealRepository.deleteByMealDateBetween(context.startDate(), context.endDate());
    }

    private void saveToDatabase(List<Meal> uniqueMeals) {
        List<Meal> savedMeals = mealRepository.saveAll(uniqueMeals);
        mealRepository.flush();

        log.info("💾 저장 완료 - Meal: {}개", savedMeals.size());
    }

    private MealWeeklyResponse buildWeeklyResponse(LocalDate startDate, LocalDate endDate, List<MealListDto> meals) {
        Map<LocalDate, Map<MealType, String>> mealsByDate = meals.stream()
                .collect(Collectors.groupingBy(
                        MealListDto::getMealDate,
                        Collectors.toMap(
                                MealListDto::getMealType,
                                MealListDto::getMenuItems,
                                (existing, replacement) -> existing,
                                () -> new EnumMap<>(MealType.class)
                        )
                ));

        List<MealDailyResponse> dailyMeals = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> createDailyMeal(date, mealsByDate))
                .sorted(Comparator.comparing(MealDailyResponse::getDate))
                .collect(Collectors.toList());

        return MealWeeklyResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .dailyMeals(dailyMeals)
                .build();
    }

    private MealDailyResponse createDailyMeal(LocalDate date, Map<LocalDate, Map<MealType, String>> mealsByDate) {
        Map<MealType, String> dailyMealMap = mealsByDate.getOrDefault(date, Collections.emptyMap());

        return MealDailyResponse.builder()
                .date(date)
                .dayOfWeek(DayOfWeekUtil.toKorean(date.getDayOfWeek()))
                .koreanMenu(dailyMealMap.getOrDefault(MealType.KOREAN, DEFAULT_EMPTY_MENU ))
                .specialMenu(dailyMealMap.getOrDefault(MealType.SPECIAL, DEFAULT_EMPTY_MENU ))
                .build();
    }

    private record CrawlingData(List<Meal> allMeals, LocalDate lastDate, LocalDate currentWeekStart) {
    }

    private record WeekDataContext(LocalDate startDate, LocalDate endDate, String weekName) {
    }
}