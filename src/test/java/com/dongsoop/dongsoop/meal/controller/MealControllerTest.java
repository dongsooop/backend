package com.dongsoop.dongsoop.meal.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.meal.dto.MealDailyResponse;
import com.dongsoop.dongsoop.meal.dto.MealPriceResponse;
import com.dongsoop.dongsoop.meal.dto.MealWeeklyResponse;
import com.dongsoop.dongsoop.meal.service.MealService;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.memberdevice.util.DeviceUtil;
import com.dongsoop.dongsoop.notification.service.FCMService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MealController.class)
@AutoConfigureMockMvc(addFilters = false)
class MealControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MealService mealService;

    @MockitoBean
    private MemberService memberService;
    @MockitoBean
    private FCMService fcmService;
    @MockitoBean
    private JwtFilter jwtFilter;
    @MockitoBean
    private FirebaseAppCheck firebaseAppCheck;
    @MockitoBean
    private DeviceUtil deviceUtil;
    @MockitoBean
    private MemberDeviceService memberDeviceService;

    @Test
    @DisplayName("이번 주 식단 응답에 학교 공지가 함께 내려간다")
    void currentWeekIncludesNotice() throws Exception {
        LocalDate monday = LocalDate.of(2026, 9, 21);
        MealDailyResponse daily = MealDailyResponse.builder()
                .date(monday)
                .dayOfWeek("월")
                .koreanMenu("백미밥, 달걀국")
                .specialMenu("덮밥: 스팸김치볶음밥")
                .build();
        when(mealService.getCurrentWeekMealResponse()).thenReturn(MealWeeklyResponse.builder()
                .startDate(monday)
                .endDate(monday.plusDays(4))
                .dailyMeals(List.of(daily))
                .notice("15:00까지만 운영합니다.")
                .build());

        mockMvc.perform(get("/meal/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notice").value("15:00까지만 운영합니다."))
                .andExpect(jsonPath("$.dailyMeals[0].specialMenu").value("덮밥: 스팸김치볶음밥"));
    }

    @Test
    @DisplayName("가격표는 식권 가격과 분류별 메뉴를 내려주고 하루 동안 캐시된다")
    void pricesReturnCategoriesWithDayCache() throws Exception {
        MealPriceResponse.Item item = new MealPriceResponse.Item("삼겹살덮밥", 5500, 7500, List.of("수"));
        MealPriceResponse.Category category = new MealPriceResponse.Category("덮밥류", "요일별로 메뉴가 상이합니다", List.of(item));
        when(mealService.getPriceResponse()).thenReturn(new MealPriceResponse(6500, List.of(category)));

        mockMvc.perform(get("/meal/prices"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=86400, public"))
                .andExpect(jsonPath("$.ticketPrice").value(6500))
                .andExpect(jsonPath("$.categories[0].name").value("덮밥류"))
                .andExpect(jsonPath("$.categories[0].note").value("요일별로 메뉴가 상이합니다"))
                .andExpect(jsonPath("$.categories[0].items[0].name").value("삼겹살덮밥"))
                .andExpect(jsonPath("$.categories[0].items[0].price").value(5500))
                .andExpect(jsonPath("$.categories[0].items[0].largePrice").value(7500))
                .andExpect(jsonPath("$.categories[0].items[0].days[0]").value("수"));
    }
}
