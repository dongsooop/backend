package com.dongsoop.dongsoop.home;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.home.controller.HomeController;
import com.dongsoop.dongsoop.home.dto.HomeDto;
import com.dongsoop.dongsoop.home.service.HomeService;
import com.dongsoop.dongsoop.jwt.filter.JwtFilter;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.memberdevice.util.DeviceUtil;
import com.dongsoop.dongsoop.notice.preference.dto.GuestDepartmentResponse;
import com.dongsoop.dongsoop.notice.preference.service.GuestNoticePreferenceService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class GuestHomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeService homeService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private GuestNoticePreferenceService guestNoticePreferenceService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private FirebaseAppCheck firebaseAppCheck;

    @MockitoBean
    private MemberDeviceService memberDeviceService;

    @MockitoBean
    private DeviceUtil deviceUtil;

    private static final HomeDto EMPTY_HOME =
            new HomeDto(List.of(), List.of(), List.of(), List.of());

    @Test
    @DisplayName("익명 키 헤더가 없으면 기존 비로그인 홈을 그대로 반환한다")
    void falls_back_to_anonymous_home_without_header() throws Exception {
        given(homeService.getHome()).willReturn(EMPTY_HOME);

        mockMvc.perform(get("/home"))
                .andExpect(status().isOk());

        verify(homeService).getHome();
        verify(homeService, never()).getGuestHome(any());
    }

    @Test
    @DisplayName("익명 키에 학과가 설정돼 있으면 학과 홈을 반환한다")
    void uses_guest_department_when_present() throws Exception {
        given(guestNoticePreferenceService.getDepartment("key-1"))
                .willReturn(new GuestDepartmentResponse(DepartmentType.DEPT_2001));
        given(homeService.getGuestHome(DepartmentType.DEPT_2001)).willReturn(EMPTY_HOME);

        mockMvc.perform(get("/home").header("X-Anonymous-Key", "key-1"))
                .andExpect(status().isOk());

        verify(homeService).getGuestHome(DepartmentType.DEPT_2001);
        verify(homeService, never()).getHome();
    }

    @Test
    @DisplayName("익명 키는 있지만 학과가 없으면 기존 비로그인 홈을 반환한다")
    void falls_back_when_department_not_set() throws Exception {
        given(guestNoticePreferenceService.getDepartment("key-2"))
                .willReturn(new GuestDepartmentResponse(null));
        given(homeService.getHome()).willReturn(EMPTY_HOME);

        mockMvc.perform(get("/home").header("X-Anonymous-Key", "key-2"))
                .andExpect(status().isOk());

        verify(homeService).getHome();
    }
}
