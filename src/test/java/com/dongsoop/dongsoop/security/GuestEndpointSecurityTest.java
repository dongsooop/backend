package com.dongsoop.dongsoop.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 비회원 설정 API 를 실제 보안 필터 체인을 통과시켜 검증한다.
 *
 * <p>다른 비회원 테스트는 서비스를 직접 부르거나 {@code addFilters = false} 로 필터를 끄기 때문에,
 * application.yml 의 허용 경로 목록이 깨지거나 헤더명이 바뀌어도 잡히지 않는다.
 * 허용 경로는 콤마로 구분된 한 줄 문자열이라 조용히 어긋나기 쉽다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("비회원 엔드포인트 보안 및 헤더 계약 통합 테스트")
class GuestEndpointSecurityTest {

    private static final String DEVICE_TOKEN_HEADER = "X-Device-Token";

    // 모든 요청은 App Check 필터를 먼저 통과해야 한다. FirebaseAppCheck 는 목이라 값 자체는 검증되지 않는다
    private static final String APP_CHECK_HEADER = "X-Firebase-AppCheck";
    private static final String APP_CHECK_TOKEN = "app-check-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @MockitoBean
    private DeviceBlacklistService deviceBlacklistService;

    @MockitoBean
    private FirebaseAppCheck firebaseAppCheck;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    private String deviceToken;

    @BeforeEach
    void setUp() {
        departmentRepository.save(new Department(DepartmentType.DEPT_2001, "컴퓨터소프트웨어공학과",
                "https://example.test/notice"));

        deviceToken = "token-security-test";
        MemberDevice device = MemberDevice.builder()
                .deviceToken(deviceToken)
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();
        memberDeviceRepository.save(device);
    }

    @Test
    @DisplayName("학과 설정과 조회가 인증 없이 통과한다")
    void guest_department_endpoints_are_permitted_without_authentication() throws Exception {
        mockMvc.perform(put("/guest/departments")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentTypes\":[\"DEPT_2001\"]}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/guest/departments")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, deviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentTypes[0]").value("DEPT_2001"));
    }

    /**
     * 헤더명이 바뀌면 이 테스트만 실패한다. 401 이 아니라 404 여야 허용 경로는 살아 있고
     * 헤더 계약만 깨졌다는 뜻이 되므로, 두 실패 원인이 구분된다.
     */
    @Test
    @DisplayName("디바이스 토큰 헤더가 없으면 401이 아니라 404를 반환한다")
    void missing_device_token_header_returns_not_found_not_unauthorized() throws Exception {
        mockMvc.perform(get("/guest/departments")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("알 수 없는 디바이스 토큰은 404를 반환한다")
    void unknown_device_token_returns_not_found() throws Exception {
        mockMvc.perform(get("/guest/departments")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, "no-such-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("departmentTypes가 빈 배열이면 400을 반환한다 (@NotEmpty)")
    void empty_department_types_returns_bad_request() throws Exception {
        mockMvc.perform(put("/guest/departments")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentTypes\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 학과 문자열이 오면 500이 아니라 400을 반환한다")
    void unknown_department_type_string_returns_bad_request_not_server_error() throws Exception {
        mockMvc.perform(put("/guest/departments")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentTypes\":[\"NOT_A_REAL_DEPARTMENT\"]}"))
                .andExpect(status().isBadRequest());
    }
}
