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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealServiceImpl implements MealService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int CLEANUP_WEEKS = 2;
    private static final int DEFAULT_WEEKS_BACK = 1;

    private static final Function<LocalDate, LocalDate> GET_WEEK_START = date -> date.with(DayOfWeek.MONDAY);
    private static final Function<LocalDate, LocalDate> GET_WEEK_END = date -> date.with(DayOfWeek.FRIDAY);

    private static final Map<Boolean, BiConsumer<Object, Integer>> LOG_STRATEGIES = Map.of(
            true, (context, count) -> log.info("✅ 처리 완료: {} 건", count),
            false, (context, count) -> log.info("ℹ️ 처리할 데이터가 없습니다.")
    );

    private final MealRepository mealRepository;
    private final MealDetailsRepository mealDetailsRepository;
    private final MealParser mealParser;

    @Value("${meal.crawler.url}")
    private String mealUrl;
    @Value("${meal.crawler.timeout:15000}")
    private int connectionTimeout;
    @Value("${meal.crawler.user-agent}")
    private String userAgent;

    @Override
    @Transactional(readOnly = true)
    public MealWeeklyResponse getCurrentWeekMealResponse() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = GET_WEEK_START.apply(today);
        LocalDate endDate = GET_WEEK_END.apply(today);

        List<MealListDto> meals = mealDetailsRepository.findMealsByDateRangeList(startDate, endDate);

        return Optional.of(meals)
                .filter(list -> !list.isEmpty())
                .map(list -> buildWeeklyMealResponse(startDate, endDate, list))
                .orElseThrow(() -> new MealNotFoundException(startDate, endDate));
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

    private void executeScheduledTask(String taskName, Runnable task) {
        logTaskStart(taskName);

        try {
            task.run();
        } catch (Exception e) {
            log.error("❌ {} 중 오류 발생", taskName, e);
        }

        logTaskEnd(taskName);
    }

    private void performCrawling() {
        LocalDate lastDate = getLastCrawledDate();
        LocalDate currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        Document document = fetchMealDocument();

        List<MealDetails> weeklyMeals = mealParser.parseWeeklyMeal(document);

        boolean isFirstCrawling = isFirstCrawling();
        List<MealDetails> newMeals = isFirstCrawling ?
                weeklyMeals :
                filterNewMeals(weeklyMeals, lastDate, currentWeekStart);

        log.info("📊 크롤링 결과 - 전체: {}개, 새로운 데이터: {}개 (첫 크롤링: {})",
                weeklyMeals.size(), newMeals.size(), isFirstCrawling);
        processDataWithLogging(newMeals, this::saveMealData);
    }

    private boolean isFirstCrawling() {
        return mealDetailsRepository.count() == 0;
    }

    private List<MealDetails> filterNewMeals(List<MealDetails> weeklyMeals, LocalDate lastDate, LocalDate currentWeekStart) {
        return weeklyMeals.stream()
                .filter(meal -> {
                    LocalDate mealDate = meal.getMealDate();
                    boolean isCurrentWeek = !mealDate.isBefore(currentWeekStart);
                    boolean isAfterLastDate = mealDate.isAfter(lastDate);

                    log.debug("🗓️ 필터링 체크 - 날짜: {}, 현재주: {}, 최신이후: {}",
                            mealDate, isCurrentWeek, isAfterLastDate);

                    return isCurrentWeek || isAfterLastDate;
                })
                .collect(Collectors.toList());
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
        return mealDetailsRepository.findMaxMealDate()
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
        List<MealDetails> uniqueDetails = removeDuplicates(newMealDetails);

        Optional<LocalDate> currentWeekStart = uniqueDetails.stream()
                .map(MealDetails::getMealDate)
                .filter(date -> !date.isBefore(LocalDate.now().with(DayOfWeek.MONDAY)))
                .findFirst();

        if (currentWeekStart.isPresent()) {
            LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
            LocalDate weekEnd = LocalDate.now().with(DayOfWeek.FRIDAY);
            log.info("🔄 현재 주({} ~ {}) 기존 데이터 삭제 후 새로 저장", weekStart, weekEnd);
            mealDetailsRepository.deleteByMealDateBetween(weekStart, weekEnd);
        }

        List<Meal> mealList = createMealEntities(uniqueDetails);

        mealDetailsRepository.saveAll(uniqueDetails);
        mealRepository.saveAll(mealList);
        mealRepository.flush();

        log.info("💾 저장 완료 - MealDetails: {}개, Meal: {}개", uniqueDetails.size(), mealList.size());
    }

    private List<MealDetails> removeDuplicates(List<MealDetails> mealDetails) {
        return new ArrayList<>(new LinkedHashSet<>(mealDetails));
    }

    private List<Meal> createMealEntities(List<MealDetails> mealDetails) {
        return mealDetails.stream()
                .map(Meal::new)
                .collect(Collectors.toList());
    }

    private MealWeeklyResponse buildWeeklyMealResponse(LocalDate startDate, LocalDate endDate,
                                                       List<MealListDto> meals) {
        Map<LocalDate, Map<MealType, String>> mealsByDate = groupMealsByDate(meals);
        List<MealDailyResponse> dailyMeals = createDailyMealResponses(startDate, endDate, mealsByDate);

        return MealWeeklyResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .dailyMeals(dailyMeals)
                .build();
    }

    private Map<LocalDate, Map<MealType, String>> groupMealsByDate(List<MealListDto> meals) {
        return meals.stream()
                .collect(Collectors.groupingBy(
                        MealListDto::getMealDate,
                        Collectors.toMap(
                                MealListDto::getMealType,
                                MealListDto::getMenuItems,
                                (existing, replacement) -> existing,
                                () -> new EnumMap<>(MealType.class)
                        )
                ));
    }

    private List<MealDailyResponse> createDailyMealResponses(LocalDate startDate, LocalDate endDate,
                                                             Map<LocalDate, Map<MealType, String>> mealsByDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .map(date -> createDailyMeal(date, mealsByDate))
                .sorted(Comparator.comparing(MealDailyResponse::getDate))
                .collect(Collectors.toList());
    }

    private MealDailyResponse createDailyMeal(LocalDate date, Map<LocalDate, Map<MealType, String>> mealsByDate) {
        Map<MealType, String> dailyMealMap = mealsByDate.getOrDefault(date, Collections.emptyMap());

        return MealDailyResponse.builder()
                .date(date)
                .dayOfWeek(DayOfWeekUtil.toKorean(date.getDayOfWeek()))
                .koreanMenu(dailyMealMap.getOrDefault(MealType.KOREAN, "식단 정보 없음"))
                .specialMenu(dailyMealMap.getOrDefault(MealType.SPECIAL, "식단 정보 없음"))
                .build();
    }

    private void logTaskStart(String taskName) {
        log.info("=== {} 시작 === [{}]", taskName, LocalDateTime.now().format(DATE_TIME_FORMATTER));
    }

    private void logTaskEnd(String taskName) {
        log.info("=== {} 종료 === [{}]", taskName, LocalDateTime.now().format(DATE_TIME_FORMATTER));
    }
}