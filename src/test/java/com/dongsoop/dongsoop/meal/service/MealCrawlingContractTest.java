package com.dongsoop.dongsoop.meal.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.meal.config.MealPriceProperties;
import com.dongsoop.dongsoop.meal.entity.Meal;
import com.dongsoop.dongsoop.meal.entity.MealNotice;
import com.dongsoop.dongsoop.meal.entity.MealType;
import com.dongsoop.dongsoop.meal.repository.MealNoticeRepository;
import com.dongsoop.dongsoop.meal.repository.MealRepository;
import com.dongsoop.dongsoop.meal.util.MealParser;
import com.dongsoop.dongsoop.meal.util.UrlEncodingUtil;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MealCrawlingContractTest {
  @Mock MealRepository meals;
  @Mock MealNoticeRepository notices;
  @Mock MealParser parser;
  @Mock UrlEncodingUtil urls;
  private MealServiceImpl service;
  private HttpServer server;
  private String base;
  private final AtomicInteger requests = new AtomicInteger();
  private final LocalDate monday = LocalDate.now(ZoneId.of("Asia/Seoul")).with(DayOfWeek.MONDAY);

  @BeforeEach
  void setup() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          requests.incrementAndGet();
          byte[] body = "<html><title>meal fixture</title></html>".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          try (var output = exchange.getResponseBody()) {
            output.write(body);
          }
          exchange.close();
        });
    server.start();
    base = "http://127.0.0.1:" + server.getAddress().getPort();
    service =
        new MealServiceImpl(meals, notices, parser, urls, new MealPriceProperties(6500, List.of()));
    ReflectionTestUtils.setField(service, "mealBaseUrl", base);
    ReflectionTestUtils.setField(service, "connectionTimeout", 2000);
    ReflectionTestUtils.setField(service, "userAgent", "meal-contract-test");
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void crawlingDeduplicatesDateAndTypeAndAdjustsRepeatedNextWeek(boolean repeatedCurrentWeek) {
    routes();
    LocalDate nextDate = monday.plusWeeks(1);
    if (repeatedCurrentWeek) {
      nextDate = monday;
    }
    when(parser.parseWeeklyMeal(any(Document.class)))
        .thenReturn(List.of(meal(monday, "first"), meal(monday, "duplicate")))
        .thenReturn(List.of(meal(nextDate, "next")));
    when(parser.parseNotice(any(Document.class)))
        .thenReturn(Optional.of("current notice"))
        .thenReturn(Optional.of("next notice"));
    service.crawlWeeklyMeal();
    ArgumentCaptor<Iterable<Meal>> saved = mealCaptor();
    verify(meals).saveAll(saved.capture());
    assertThat(saved.getValue())
        .extracting(Meal::getMealDate)
        .containsExactly(monday, monday.plusWeeks(1));
    assertThat(saved.getValue()).extracting(Meal::getMenuItems).containsExactly("first", "next");
    verify(meals).deleteByMealDateBetween(monday, monday.plusDays(4));
    verify(meals).deleteByMealDateBetween(monday.plusWeeks(1), monday.plusWeeks(1).plusDays(4));
    verify(meals).flush();
    ArgumentCaptor<MealNotice> notice = ArgumentCaptor.forClass(MealNotice.class);
    verify(notices, times(2)).save(notice.capture());
    assertThat(notice.getAllValues())
        .extracting(MealNotice::getWeekStart)
        .containsExactly(monday, monday.plusWeeks(1));
    assertThat(notice.getAllValues())
        .extracting(MealNotice::getContent)
        .containsExactly("current notice", "next notice");
    assertThat(requests.get()).isEqualTo(2);
  }

  @Test
  void existingWeekNoticeIsUpdatedAndRemovedNextWeekNoticeIsDeleted() {
    routes();
    when(parser.parseWeeklyMeal(any(Document.class)))
        .thenReturn(List.of(meal(monday, "current")))
        .thenReturn(List.of(meal(monday.plusWeeks(1), "next")));
    when(parser.parseNotice(any(Document.class)))
        .thenReturn(Optional.of("updated"))
        .thenReturn(Optional.empty());
    MealNotice existing = new MealNotice(monday, "old");
    when(notices.findByWeekStart(monday)).thenReturn(Optional.of(existing));
    when(meals.count()).thenReturn(10L);
    when(meals.findMaxMealDate()).thenReturn(Optional.of(monday.plusWeeks(2)));
    service.crawlWeeklyMeal();
    assertThat(existing.getContent()).isEqualTo("updated");
    verify(notices).deleteByWeekStart(monday.plusWeeks(1));
    verify(notices, never()).save(any());
    ArgumentCaptor<Iterable<Meal>> saved = mealCaptor();
    verify(meals).saveAll(saved.capture());
    assertThat(saved.getValue()).hasSize(2);
  }

  @Test
  void emptyParsedWeeksDoNotDeleteOrSaveExistingData() {
    routes();
    when(parser.parseWeeklyMeal(any(Document.class))).thenReturn(List.of());
    when(parser.parseNotice(any(Document.class))).thenReturn(Optional.empty());
    service.crawlWeeklyMeal();
    verify(meals, never()).saveAll(any());
    verify(meals, never()).deleteByMealDateBetween(any(), any());
    verifyNoInteractions(notices);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void retryOnlyCrawlsWhenRequestedWeekHasNoRealMenu(boolean available) {
    when(meals.existsByMealDateBetweenAndMenuItemsNot(any(), any(), eq("식단 정보 없음")))
        .thenReturn(available);
    if (!available) {
      routes();
      when(parser.parseWeeklyMeal(any(Document.class))).thenReturn(List.of());
      when(parser.parseNotice(any(Document.class))).thenReturn(Optional.empty());
    }
    service.retryNextWeekMeal();
    service.retryMissingWeeklyMeal();
    verify(meals).existsByMealDateBetweenAndMenuItemsNot(monday, monday.plusDays(4), "식단 정보 없음");
    verify(meals)
        .existsByMealDateBetweenAndMenuItemsNot(
            monday.plusWeeks(1), monday.plusWeeks(1).plusDays(4), "식단 정보 없음");
    int expectedRequests = 4;
    if (available) {
      expectedRequests = 0;
    }
    assertThat(requests.get()).isEqualTo(expectedRequests);
  }

  @Test
  void httpFailureDoesNotEscapeSchedulerOrWritePartialData() {
    server.removeContext("/");
    server.createContext(
        "/",
        exchange -> {
          exchange.sendResponseHeaders(500, -1);
          exchange.close();
        });
    when(urls.buildWeekUrl(base, monday.plusWeeks(1), "pre")).thenReturn(base + "/current");
    assertThatCode(service::crawlWeeklyMeal).doesNotThrowAnyException();
    verifyNoInteractions(parser, meals, notices);
  }

  @Test
  void cleanupUsesSameCutoffAndFailureDoesNotEscapeScheduler() {
    LocalDate cutoff = LocalDate.now(ZoneId.of("Asia/Seoul")).minusWeeks(2);
    service.cleanupOldMealData();
    verify(meals).deleteOldMealData(cutoff);
    verify(notices).deleteOldNotices(cutoff);
    doThrow(new IllegalStateException("database unavailable"))
        .when(meals)
        .deleteOldMealData(cutoff);
    assertThatCode(service::cleanupOldMealData).doesNotThrowAnyException();
  }

  private void routes() {
    when(urls.buildWeekUrl(base, monday.plusWeeks(1), "pre")).thenReturn(base + "/current");
    when(urls.buildWeekUrl(base, monday, "next")).thenReturn(base + "/next");
  }

  private Meal meal(LocalDate date, String menu) {
    return Meal.builder()
        .mealDate(date)
        .dayOfWeek("월")
        .mealType(MealType.KOREAN)
        .menuItems(menu)
        .build();
  }

  @SuppressWarnings("unchecked")
  private ArgumentCaptor<Iterable<Meal>> mealCaptor() {
    return ArgumentCaptor.forClass(Iterable.class);
  }
}
