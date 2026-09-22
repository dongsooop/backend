package com.dongsoop.dongsoop.meal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.meal.config.MealPriceProperties;
import com.dongsoop.dongsoop.meal.dto.MealListDto;
import com.dongsoop.dongsoop.meal.dto.MealPriceResponse;
import com.dongsoop.dongsoop.meal.dto.MealWeeklyResponse;
import com.dongsoop.dongsoop.meal.entity.MealNotice;
import com.dongsoop.dongsoop.meal.entity.MealType;
import com.dongsoop.dongsoop.meal.exception.MealNotFoundException;
import com.dongsoop.dongsoop.meal.repository.MealNoticeRepository;
import com.dongsoop.dongsoop.meal.repository.MealRepository;
import com.dongsoop.dongsoop.meal.util.MealParser;
import com.dongsoop.dongsoop.meal.util.UrlEncodingUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MealServiceImplTest {

    @Mock
    private MealRepository mealRepository;
    @Mock
    private MealNoticeRepository mealNoticeRepository;
    @Mock
    private MealParser mealParser;
    @Mock
    private UrlEncodingUtil urlEncodingUtil;

    @Test
    @DisplayName("이번 주 식단에 저장된 공지가 있으면 응답에 담는다")
    void currentWeekIncludesStoredNotice() {
        MealServiceImpl service = service(new MealPriceProperties(6500, List.of()));
        when(mealRepository.findMealsByDateRangeList(any(), any()))
                .thenReturn(List.of(meal(LocalDate.now(), MealType.KOREAN, "백미밥")));
        when(mealNoticeRepository.findByWeekStart(any()))
                .thenReturn(Optional.of(new MealNotice(LocalDate.now(), "15:00까지만 운영합니다.")));

        MealWeeklyResponse response = service.getCurrentWeekMealResponse();

        assertEquals("15:00까지만 운영합니다.", response.getNotice());
    }

    @Test
    @DisplayName("공지가 없으면 notice 는 null 이고 식단은 그대로 내려간다")
    void currentWeekWithoutNotice() {
        MealServiceImpl service = service(new MealPriceProperties(6500, List.of()));
        when(mealRepository.findMealsByDateRangeList(any(), any()))
                .thenReturn(List.of(meal(LocalDate.now(), MealType.KOREAN, "백미밥")));
        when(mealNoticeRepository.findByWeekStart(any())).thenReturn(Optional.empty());

        MealWeeklyResponse response = service.getCurrentWeekMealResponse();

        assertNull(response.getNotice());
        assertEquals(5, response.getDailyMeals().size());
    }

    @Test
    @DisplayName("이번 주 식단이 하나도 없으면 공지가 있어도 MealNotFoundException 을 던진다")
    void currentWeekWithoutMealsThrows() {
        MealServiceImpl service = service(new MealPriceProperties(6500, List.of()));
        when(mealRepository.findMealsByDateRangeList(any(), any())).thenReturn(List.of());
        when(mealNoticeRepository.findByWeekStart(any()))
                .thenReturn(Optional.of(new MealNotice(LocalDate.now(), "공지")));

        assertThrows(MealNotFoundException.class, service::getCurrentWeekMealResponse);
    }

    @Test
    @DisplayName("가격표는 설정값을 그대로 옮기고 비어 있는 요일·목록은 빈 배열로 채운다")
    void priceResponseFillsMissingListsWithEmpty() {
        MealPriceProperties.Item withDays = new MealPriceProperties.Item("삼겹살덮밥", 5500, 7500, List.of("수"));
        MealPriceProperties.Item withoutDays = new MealPriceProperties.Item("신라면", 3500, null, null);
        MealPriceProperties properties = new MealPriceProperties(6500, List.of(
                new MealPriceProperties.Category("덮밥류", "요일별로 메뉴가 상이합니다", List.of(withDays)),
                new MealPriceProperties.Category("라면류", null, List.of(withoutDays)),
                new MealPriceProperties.Category("음료류", null, null)));
        MealServiceImpl service = service(properties);

        MealPriceResponse response = service.getPriceResponse();

        assertEquals(6500, response.ticketPrice());
        assertEquals(3, response.categories().size());
        MealPriceResponse.Item first = response.categories().get(0).items().get(0);
        assertEquals(List.of("수"), first.days());
        assertEquals(7500, first.largePrice());
        MealPriceResponse.Item second = response.categories().get(1).items().get(0);
        assertEquals(List.of(), second.days());
        assertNull(second.largePrice());
        assertEquals(List.of(), response.categories().get(2).items());
    }

    @Test
    @DisplayName("가격 분류가 설정에 없으면 빈 목록을 돌려준다")
    void priceResponseWithoutCategories() {
        MealServiceImpl service = service(new MealPriceProperties(6500, null));

        MealPriceResponse response = service.getPriceResponse();

        assertEquals(List.of(), response.categories());
    }

    private MealServiceImpl service(MealPriceProperties properties) {
        return new MealServiceImpl(mealRepository, mealNoticeRepository, mealParser, urlEncodingUtil, properties);
    }

    private MealListDto meal(LocalDate date, MealType type, String menu) {
        return MealListDto.builder()
                .id(1L)
                .mealDate(date)
                .dayOfWeek("월")
                .mealType(type)
                .menuItems(menu)
                .build();
    }
}
