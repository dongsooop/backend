package com.dongsoop.dongsoop.meal.service;

import com.dongsoop.dongsoop.meal.config.MealPriceProperties;
import com.dongsoop.dongsoop.meal.dto.MealDailyResponse;
import com.dongsoop.dongsoop.meal.dto.MealListDto;
import com.dongsoop.dongsoop.meal.dto.MealPriceResponse;
import com.dongsoop.dongsoop.meal.dto.MealWeeklyResponse;
import com.dongsoop.dongsoop.meal.entity.Meal;
import com.dongsoop.dongsoop.meal.entity.MealNotice;
import com.dongsoop.dongsoop.meal.entity.MealType;
import com.dongsoop.dongsoop.meal.exception.MealCrawlingException;
import com.dongsoop.dongsoop.meal.exception.MealNotFoundException;
import com.dongsoop.dongsoop.meal.repository.MealNoticeRepository;
import com.dongsoop.dongsoop.meal.repository.MealRepository;
import com.dongsoop.dongsoop.meal.util.DayOfWeekUtil;
import com.dongsoop.dongsoop.meal.util.MealParser;
import com.dongsoop.dongsoop.meal.util.UrlEncodingUtil;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MealServiceImpl implements MealService {

    private static final String DEFAULT_EMPTY_MENU = "식단 정보 없음";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MealRepository mealRepository;
    private final MealNoticeRepository mealNoticeRepository;
    private final MealParser mealParser;
    private final UrlEncodingUtil urlEncodingUtil;
    private final MealPriceProperties mealPriceProperties;

    @Value("${meal.crawler.base-url}")
    private String mealBaseUrl;

    @Value("${meal.crawler.timeout:15000}")
    private int connectionTimeout;

    @Value("${meal.crawler.user-agent}")
    private String userAgent;

    @Override
    @Transactional(readOnly = true)
    public MealWeeklyResponse getCurrentWeekMealResponse() {
        LocalDate today = LocalDate.now(KST);
        LocalDate startDate = today.with(DayOfWeek.MONDAY);
        LocalDate endDate = today.with(DayOfWeek.FRIDAY);

        List<MealListDto> meals = mealRepository.findMealsByDateRangeList(startDate, endDate);
        String notice = mealNoticeRepository.findByWeekStart(startDate)
                .map(MealNotice::getContent)
                .orElse(null);

        return Optional.of(meals)
                .filter(list -> !list.isEmpty())
                .map(list -> buildWeeklyResponse(startDate, endDate, list, notice))
                .orElseThrow(() -> new MealNotFoundException(startDate, endDate));
    }

    @Override
    public MealPriceResponse getPriceResponse() {
        List<MealPriceResponse.Category> categories = Optional.ofNullable(mealPriceProperties.categories())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toCategoryResponse)
                .toList();

        return new MealPriceResponse(mealPriceProperties.ticketPrice(), categories);
    }

    private MealPriceResponse.Category toCategoryResponse(MealPriceProperties.Category category) {
        List<MealPriceResponse.Item> items = Optional.ofNullable(category.items())
                .orElse(Collections.emptyList())
                .stream()
                .map(item -> new MealPriceResponse.Item(
                        item.name(),
                        item.price(),
                        item.largePrice(),
                        Optional.ofNullable(item.days()).orElse(Collections.emptyList())))
                .toList();

        return new MealPriceResponse.Category(category.name(), category.note(), items);
    }

    @Scheduled(cron = "0 0 9 * * SAT", zone = "Asia/Seoul")
    @Transactional
    public void crawlWeeklyMeal() {
        executeTask("자동 식단 크롤링", this::performCrawling);
    }

    // 토요일 크롤링이 실패했으면 일요일에 한 번 더 시도
    @Scheduled(cron = "0 0 9 * * SUN", zone = "Asia/Seoul")
    @Transactional
    public void retryNextWeekMeal() {
        crawlIfMealMissing(LocalDate.now(KST).with(DayOfWeek.MONDAY).plusWeeks(1));
    }

    // 월요일에 이번 주 식단이 비어 있으면 채워질 때까지 1시간마다 재시도
    @Scheduled(cron = "0 30 * * * MON", zone = "Asia/Seoul")
    @Transactional
    public void retryMissingWeeklyMeal() {
        crawlIfMealMissing(LocalDate.now(KST).with(DayOfWeek.MONDAY));
    }

    private void crawlIfMealMissing(LocalDate monday) {
        if (mealRepository.existsByMealDateBetweenAndMenuItemsNot(monday, monday.plusDays(4), DEFAULT_EMPTY_MENU)) {
            return;
        }

        executeTask("식단 재크롤링", this::performCrawling);
    }

    @Scheduled(cron = "0 0 2 * * SUN", zone = "Asia/Seoul")
    @Transactional
    public void cleanupOldMealData() {
        executeTask("자동 식단 데이터 정리", this::performCleanup);
    }

    private void executeTask(String taskName, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("❌ {} 중 오류 발생", taskName, e);
        }
    }

    private void performCrawling() {
        LocalDate today = LocalDate.now(KST);
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        CrawledWeek currentWeek = crawlCurrentWeek(monday);
        CrawledWeek nextWeek = crawlNextWeek(monday);

        List<Meal> allMeals = new ArrayList<>();
        allMeals.addAll(currentWeek.meals());
        allMeals.addAll(nextWeek.meals());

        boolean isFirstCrawling = mealRepository.count() == 0;
        LocalDate lastDate = mealRepository.findMaxMealDate()
                .orElse(today.minusWeeks(1));
        LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);

        List<Meal> newMeals = selectNewMeals(isFirstCrawling, allMeals, lastDate, currentWeekStart);

        Optional.of(newMeals)
                .filter(meals -> !meals.isEmpty())
                .ifPresent(this::saveMealData);

        saveNotice(currentWeek);
        saveNotice(nextWeek);
    }

    private CrawledWeek crawlCurrentWeek(LocalDate monday) {
        LocalDate nextMonday = monday.plusWeeks(1);
        String url = urlEncodingUtil.buildWeekUrl(mealBaseUrl, nextMonday, "pre");

        return crawlWeek(url);
    }

    private CrawledWeek crawlNextWeek(LocalDate monday) {
        String url = urlEncodingUtil.buildWeekUrl(mealBaseUrl, monday, "next");

        CrawledWeek crawled = crawlWeek(url);
        List<Meal> meals = crawled.meals();

        List<Meal> processedMeals = Optional.of(meals)
                .filter(mealList -> isCurrentWeekData(mealList, monday))
                .map(mealList -> adjustToNextWeek(mealList, monday.plusWeeks(1)))
                .orElse(meals);

        return new CrawledWeek(processedMeals, crawled.notice());
    }

    private CrawledWeek crawlWeek(String url) {
        Document document = fetchDocument(url);
        List<Meal> meals = mealParser.parseWeeklyMeal(document);
        String notice = mealParser.parseNotice(document).orElse(null);

        return new CrawledWeek(meals, notice);
    }

    private boolean isCurrentWeekData(List<Meal> meals, LocalDate monday) {
        LocalDate currentWeekEnd = monday.plusDays(4);
        return meals.stream().allMatch(meal -> isDateInRange(meal.getMealDate(), monday, currentWeekEnd));
    }

    private boolean isDateInRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private List<Meal> adjustToNextWeek(List<Meal> meals, LocalDate nextWeekStart) {
        return meals.stream()
                .map(meal -> createAdjustedMeal(meal, nextWeekStart))
                .toList();
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

    private List<Meal> selectNewMeals(boolean isFirstCrawling, List<Meal> allMeals, LocalDate lastDate,
                                      LocalDate currentWeekStart) {
        return Optional.of(isFirstCrawling)
                .filter(Boolean::booleanValue)
                .map(unused -> allMeals)
                .orElseGet(() -> filterNewMeals(allMeals, lastDate, currentWeekStart));
    }

    private List<Meal> filterNewMeals(List<Meal> allMeals, LocalDate lastDate, LocalDate currentWeekStart) {
        return allMeals.stream()
                .filter(meal -> isCurrentWeekOrAfterLastDate(meal, lastDate, currentWeekStart))
                .toList();
    }

    private boolean isCurrentWeekOrAfterLastDate(Meal meal, LocalDate lastDate, LocalDate currentWeekStart) {
        LocalDate mealDate = meal.getMealDate();
        return !mealDate.isBefore(currentWeekStart) || mealDate.isAfter(lastDate);
    }

    private void performCleanup() {
        LocalDate cutoffDate = LocalDate.now(KST).minusWeeks(2);
        mealRepository.deleteOldMealData(cutoffDate);
        mealNoticeRepository.deleteOldNotices(cutoffDate);
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

    // 페이지에 공지가 없으면 그 주의 이전 공지도 지운다. 학교가 공지를 내린 뒤에도 앱에 남지 않도록
    private void saveNotice(CrawledWeek week) {
        if (week.meals().isEmpty()) {
            return;
        }

        LocalDate weekStart = week.meals().get(0).getMealDate().with(DayOfWeek.MONDAY);
        String notice = week.notice();

        if (notice == null) {
            mealNoticeRepository.deleteByWeekStart(weekStart);
            return;
        }

        mealNoticeRepository.findByWeekStart(weekStart)
                .ifPresentOrElse(
                        existing -> existing.updateContent(notice),
                        () -> mealNoticeRepository.save(new MealNotice(weekStart, notice)));
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
        LocalDate now = LocalDate.now(KST);
        LocalDate currentWeekStart = now.with(DayOfWeek.MONDAY);
        LocalDate currentWeekEnd = now.with(DayOfWeek.FRIDAY);
        LocalDate nextWeekStart = currentWeekStart.plusWeeks(1);
        LocalDate nextWeekEnd = currentWeekEnd.plusWeeks(1);

        Optional.of(meals)
                .filter(mealList -> mealList.stream().anyMatch(meal ->
                        isDateInRange(meal.getMealDate(), currentWeekStart, currentWeekEnd)))
                .ifPresent(mealList -> deleteWeekData(currentWeekStart, currentWeekEnd));

        Optional.of(meals)
                .filter(mealList -> mealList.stream().anyMatch(meal ->
                        isDateInRange(meal.getMealDate(), nextWeekStart, nextWeekEnd)))
                .ifPresent(mealList -> deleteWeekData(nextWeekStart, nextWeekEnd));
    }

    private void deleteWeekData(LocalDate startDate, LocalDate endDate) {
        mealRepository.deleteByMealDateBetween(startDate, endDate);
    }

    private void saveToDatabase(List<Meal> uniqueMeals) {
        mealRepository.saveAll(uniqueMeals);
        mealRepository.flush();
    }

    private MealWeeklyResponse buildWeeklyResponse(LocalDate startDate, LocalDate endDate, List<MealListDto> meals,
                                                   String notice) {
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
                .toList();

        return MealWeeklyResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .dailyMeals(dailyMeals)
                .notice(notice)
                .build();
    }

    private MealDailyResponse createDailyMeal(LocalDate date, Map<LocalDate, Map<MealType, String>> mealsByDate) {
        Map<MealType, String> dailyMealMap = mealsByDate.getOrDefault(date, Collections.emptyMap());

        return MealDailyResponse.builder()
                .date(date)
                .dayOfWeek(DayOfWeekUtil.toKorean(date.getDayOfWeek()))
                .koreanMenu(dailyMealMap.getOrDefault(MealType.KOREAN, DEFAULT_EMPTY_MENU))
                .specialMenu(dailyMealMap.getOrDefault(MealType.SPECIAL, DEFAULT_EMPTY_MENU))
                .build();
    }

    private record CrawledWeek(List<Meal> meals, String notice) {
    }
}
