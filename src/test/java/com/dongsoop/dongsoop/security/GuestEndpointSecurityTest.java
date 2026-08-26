package com.dongsoop.dongsoop.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    private static final String ANONYMOUS_KEY_HEADER = "X-Anonymous-Key";

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

    private String anonymousKey;

    @BeforeEach
    void setUp() {
        departmentRepository.save(new Department(DepartmentType.DEPT_2001, "컴퓨터소프트웨어공학과",
                "https://example.test/notice"));

        MemberDevice device = MemberDevice.builder()
                .deviceToken("token-security-test")
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();
        anonymousKey = device.issueAnonymousKeyIfAbsent();
        memberDeviceRepository.save(device);
    }

    @Test
    @DisplayName("학과 설정과 조회가 인증 없이 통과한다")
    void guest_department_endpoints_are_permitted_without_authentication() throws Exception {
        mockMvc.perform(put("/guest/department")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(ANONYMOUS_KEY_HEADER, anonymousKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentType\":\"DEPT_2001\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/guest/department")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(ANONYMOUS_KEY_HEADER, anonymousKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentType").value("DEPT_2001"));
    }

    @Test
    @DisplayName("키워드 목록·추가·삭제가 인증 없이 통과한다")
    void guest_keyword_endpoints_are_permitted_without_authentication() throws Exception {
        String created = mockMvc.perform(post("/guest/notice/keywords")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(ANONYMOUS_KEY_HEADER, anonymousKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"장학\",\"type\":\"INCLUDE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.keyword").value("장학"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(get("/guest/notice/keywords")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(ANONYMOUS_KEY_HEADER, anonymousKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].keyword").value("장학"));

        Long keywordId = com.jayway.jsonpath.JsonPath.parse(created).read("$.id", Integer.class).longValue();
        mockMvc.perform(delete("/guest/notice/keywords/{keywordId}", keywordId)
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(ANONYMOUS_KEY_HEADER, anonymousKey))
                .andExpect(status().isNoContent());
    }

    /**
     * 헤더명이 바뀌면 이 테스트만 실패한다. 401 이 아니라 404 여야 허용 경로는 살아 있고
     * 헤더 계약만 깨졌다는 뜻이 되므로, 두 실패 원인이 구분된다.
     */
    @Test
    @DisplayName("익명 키 헤더가 없으면 401이 아니라 404를 반환한다")
    void missing_anonymous_key_header_returns_not_found_not_unauthorized() throws Exception {
        mockMvc.perform(get("/guest/department")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/guest/notice/keywords")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("알 수 없는 익명 키는 404를 반환한다")
    void unknown_anonymous_key_returns_not_found() throws Exception {
        mockMvc.perform(get("/guest/department")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(ANONYMOUS_KEY_HEADER, "no-such-key"))
                .andExpect(status().isNotFound());
    }
}
