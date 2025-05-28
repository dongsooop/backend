package com.dongsoop.dongsoop.meal.service;

import com.dongsoop.dongsoop.exception.domain.meal.MealCrawlingException;
import com.dongsoop.dongsoop.exception.domain.meal.MealNotFoundException;
import com.dongsoop.dongsoop.meal.dto.MealDailyResponse;
import com.dongsoop.dongsoop.meal.dto.MealListDto;
import com.dongsoop.dongsoop.meal.dto.MealWeeklyResponse;
import com.dongsoop.dongsoop.meal.entity.Meal;
import com.dongsoop.dongsoop.meal.entity.MealDetails;
import com.dongsoop.dongsoop.meal.entity.MealType;
import com.dongsoop.dongsoop.meal.repository.MealDetailsRepository;
import com.dongsoop.dongsoop.meal.repository.MealRepository;
import com.dongsoop.dongsoop.meal.util.DayOfWeekUtil;
import com.dongsoop.dongsoop.meal.util.MealParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealServiceImpl implements MealService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int CLEANUP_WEEKS = 2;
    private static final int DEFAULT_WEEKS_BACK = 1;
    private static final long CACHE_TTL = 1800000; // 30분

    private static final Function<LocalDate, LocalDate> GET_WEEK_START = date -> date.with(DayOfWeek.MONDAY);
    private static final Function<LocalDate, LocalDate> GET_WEEK_END = date -> date.with(DayOfWeek.FRIDAY);
    private static final BiFunction<LocalDate, LocalDate, Predicate<MealDetails>> MEAL_DATE_FILTER =
            (lastDate, unused) -> meal -> meal.getMealDate().isAfter(lastDate);

    private static final Map<Boolean, BiConsumer<Object, Integer>> LOG_STRATEGIES = Map.of(
            true, (context, count) -> log.info("✅ 처리 완료: {} 건", count),
            false, (context, count) -> log.info("ℹ️ 처리할 데이터가 없습니다.")
    );

    private final MealRepository mealRepository;
    private final MealDetailsRepository mealDetailsRepository;
    private final MealParser mealParser;

    private final Map<String, CachedData> cache = new ConcurrentHashMap<>();

    @Value("${meal.crawler.url}")
    private String mealUrl;
    @Value("${meal.crawler.timeout:15000}")
    private int connectionTimeout;
    @Value("${meal.crawler.user-agent}")
    private String userAgent;

    @Override
    @Transactional(readOnly = true)
    public Page<MealListDto> getWeeklyMeal(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        String cacheKey = String.format("weekly:%s:%s:%d", startDate, endDate, pageable.getPageNumber());

        CachedData cached = cache.get(cacheKey);

        return Optional.ofNullable(cached)
                .filter(CachedData::isValid)
                .map(data -> (Page<MealListDto>) data.data())
                .orElseGet(() -> {
                    Page<MealListDto> meals = mealRepository.findMealsByDateRange(startDate, endDate, pageable);

                    return Optional.of(meals)
                            .filter(Page::hasContent)
                            .map(result -> {
                                cache.put(cacheKey, new CachedData(result));
                                return result;
                            })
                            .orElseThrow(() -> new MealNotFoundException(startDate, endDate));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MealListDto> getCurrentWeekMeal(Pageable pageable) {
        LocalDate today = LocalDate.now();
        return getWeeklyMeal(GET_WEEK_START.apply(today), GET_WEEK_END.apply(today), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public MealWeeklyResponse getWeeklyMealResponse(LocalDate startDate, LocalDate endDate) {
        String cacheKey = String.format("response:%s:%s", startDate, endDate);

        CachedData cached = cache.get(cacheKey);

        return Optional.ofNullable(cached)
                .filter(CachedData::isValid)
                .map(data -> (MealWeeklyResponse) data.data())
                .orElseGet(() -> {
                    List<MealListDto> meals = mealRepository.findMealsByDateRangeList(startDate, endDate);

                    return Optional.of(meals)
                            .filter(list -> !list.isEmpty())
                            .map(list -> buildWeeklyMealResponse(startDate, endDate, list))
                            .map(response -> {
                                cache.put(cacheKey, new CachedData(response));
                                return response;
                            })
                            .orElseThrow(() -> new MealNotFoundException(startDate, endDate));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public MealWeeklyResponse getCurrentWeekMealResponse() {
        LocalDate today = LocalDate.now();
        return getWeeklyMealResponse(GET_WEEK_START.apply(today), GET_WEEK_END.apply(today));
    }

    @Scheduled(cron = "0 0 9 * * FRI", zone = "Asia/Seoul")
    @Transactional
    public void crawlWeeklyMeal() {
        executeScheduledTask("자동 식단 크롤링", this::performCrawling);
    }

    @Scheduled(cron = "0 0 2 * * SAT", zone = "Asia/Seoul")
    @Transactional
    public void cleanupOldMealData() {
        executeScheduledTask("자동 식단 데이터 정리", this::performCleanup);
    }

    @Override
    @Transactional
    public void manualCrawl() {
        cache.clear(); // 수동 크롤링 시 캐시 초기화
        crawlWeeklyMeal();
    }

    @Override
    @Transactional
    public void manualCleanup() {
        cleanupOldMealData();
    }

    private void executeScheduledTask(String taskName, Runnable task) {
        logTaskStart(taskName);

        CompletableFuture.runAsync(task)
                .exceptionally(throwable -> {
                    log.error("❌ {} 중 오류 발생", taskName, throwable);
                    return null;
                })
                .join();

        logTaskEnd(taskName);
    }

    private void performCrawling() {
        Map<String, Object> crawlContext = Map.of(
                "lastDate", getLastCrawledDate(),
                "document", fetchMealDocument()
        );

        List<MealDetails> weeklyMeals = mealParser.parseWeeklyMeal((Document) crawlContext.get("document"));

        List<MealDetails> newMeals = weeklyMeals.stream()
                .filter(MEAL_DATE_FILTER.apply((LocalDate) crawlContext.get("lastDate"), null))
                .collect(Collectors.toList());

        processDataWithLogging(newMeals, this::saveMealData);
    }

    private void performCleanup() {
        LocalDate cutoffDate = LocalDate.now().minusWeeks(CLEANUP_WEEKS);
        int deletedCount = mealDetailsRepository.deleteOldMealData(cutoffDate);

        LOG_STRATEGIES.get(deletedCount > 0).accept(cutoffDate, deletedCount);
    }

    private void processDataWithLogging(List<MealDetails> data, Consumer<List<MealDetails>> processor) {
        Optional.of(data)
                .filter(list -> !list.isEmpty())
                .ifPresent(processor);

        LOG_STRATEGIES.get(!data.isEmpty()).accept(null, data.size());
    }

    private LocalDate getLastCrawledDate() {
        return mealRepository.findMaxMealDate()
                .orElse(LocalDate.now().minusWeeks(DEFAULT_WEEKS_BACK));
    }

    private Document fetchMealDocument() {
        try {
            return Jsoup.connect(mealUrl)
                    .timeout(connectionTimeout)
                    .userAgent(userAgent)
                    .headers(getDefaultHeaders())
                    .get();
        } catch (IOException e) {
            throw new MealCrawlingException(mealUrl, e);
        }
    }

    private Map<String, String> getDefaultHeaders() {
        return Map.of(
                "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                "Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.5,en;q=0.3"
        );
    }

    private void saveMealData(List<MealDetails> newMealDetails) {
        List<MealDetails> uniqueDetails = new ArrayList<>(new HashSet<>(newMealDetails));
        List<Meal> mealList = newMealDetails.parallelStream()
                .map(Meal::new)
                .collect(Collectors.toList());

        mealDetailsRepository.saveAll(uniqueDetails);
        mealRepository.saveAll(mealList);
        mealRepository.flush();
    }

    private MealWeeklyResponse buildWeeklyMealResponse(LocalDate startDate, LocalDate endDate,
                                                       List<MealListDto> meals) {
        Map<LocalDate, Map<MealType, String>> mealsByDate = meals.parallelStream()
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
                .parallel()
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
                .koreanMenu(dailyMealMap.getOrDefault(MealType.KOREAN, ""))
                .specialMenu(dailyMealMap.getOrDefault(MealType.SPECIAL, ""))
                .build();
    }

    private void logTaskStart(String taskName) {
        log.info("=== {} 시작 === [{}]", taskName, LocalDateTime.now().format(DATE_TIME_FORMATTER));
    }

    private void logTaskEnd(String taskName) {
        log.info("=== {} 종료 === [{}]", taskName, LocalDateTime.now().format(DATE_TIME_FORMATTER));
    }

    private record CachedData(Object data, long timestamp) {
        CachedData(Object data) {
            this(data, System.currentTimeMillis());
        }

        boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_TTL;
        }
    }
}