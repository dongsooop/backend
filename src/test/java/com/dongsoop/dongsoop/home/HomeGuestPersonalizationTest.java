package com.dongsoop.dongsoop.home;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.blinddate.TestJwtTokenGenerator;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepository;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepository;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.preference.entity.DeviceNoticePreference;
import com.dongsoop.dongsoop.notice.preference.repository.DeviceNoticePreferenceRepository;
import com.dongsoop.dongsoop.notice.repository.NoticeDetailsRepository;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import com.dongsoop.dongsoop.recruitment.board.dto.HomeRecruitment;
import com.dongsoop.dongsoop.recruitment.repository.RecruitmentRepository;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 비회원 홈 화면(GET /home)이 구독 학과들을 반영해 공지를 다학과로 확장해 보여주는지 검증한다.
 *
 * <p>디바이스 토큰이 없거나 알 수 없거나 회원에 바인딩됐거나 구독 학과가 없는 경우 모두
 * 개인화되지 않은 기본 홈 화면으로 안전하게 폴백해야 한다.
 *
 * <p>홈 서비스는 별도 스레드 풀에서 비동기로 조회하므로 테스트 트랜잭션(@Transactional)의
 * 미커밋 데이터는 그 스레드에서 보이지 않는다. 그래서 이 테스트는 트랜잭션 롤백 대신
 * 직접 커밋하고 끝나면 수동으로 정리한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("비회원 홈 화면 다학과 공지 개인화")
class HomeGuestPersonalizationTest {

    private static final String DEVICE_TOKEN_HEADER = "X-Device-Token";
    private static final String APP_CHECK_HEADER = "X-Firebase-AppCheck";
    private static final String APP_CHECK_TOKEN = "app-check-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @Autowired
    private DeviceNoticePreferenceRepository preferenceRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NoticeDetailsRepository noticeDetailsRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestJwtTokenGenerator testJwtTokenGenerator;

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

    @MockitoBean
    private OfficialScheduleRepository officialScheduleRepository;

    @MockitoBean
    private MemberScheduleRepository memberScheduleRepository;

    @MockitoBean
    private RecruitmentRepository recruitmentRepository;

    private final List<MemberDevice> createdDevices = new ArrayList<>();
    private final List<Member> createdMembers = new ArrayList<>();
    private final List<Notice> createdNotices = new ArrayList<>();
    private final List<NoticeDetails> createdNoticeDetails = new ArrayList<>();
    private final List<DepartmentType> createdDepartmentTypes = new ArrayList<>(List.of(
            DepartmentType.DEPT_1001, DepartmentType.DEPT_2001, DepartmentType.DEPT_3001));

    @BeforeAll
    void seedDepartmentsAndNotices() {
        // H2 테스트 환경은 실제 학사일정 조회 쿼리가 쓰는 MySQL DATE() 함수를 지원하지 않아 목으로 우회한다
        BDDMockito.given(officialScheduleRepository.searchHomeSchedule(ArgumentMatchers.any()))
                .willReturn(List.of());
        BDDMockito.given(memberScheduleRepository.searchHomeSchedule(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .willReturn(List.of());

        departmentRepository.save(new Department(DepartmentType.DEPT_1001, "대학공지", null));
        departmentRepository.save(new Department(DepartmentType.DEPT_2001, "컴퓨터소프트웨어공학과", null));
        departmentRepository.save(new Department(DepartmentType.DEPT_3001, "기계공학과", null));

        saveNotice(DepartmentType.DEPT_1001, 90001L, "대학 공지 1");
        saveNotice(DepartmentType.DEPT_2001, 90002L, "컴공 공지 1");
        saveNotice(DepartmentType.DEPT_3001, 90003L, "기계 공지 1");
    }

    @AfterAll
    void cleanUp() {
        preferenceRepository.deleteAll(preferenceRepository.findAll().stream()
                .filter(preference -> createdDevices.stream()
                        .anyMatch(device -> device.getId().equals(preference.getId().getDevice().getId())))
                .toList());
        for (Member member : createdMembers) {
            memberDeviceRepository.findAll().stream()
                    .filter(device -> member.equals(device.getMember()))
                    .forEach(memberDeviceRepository::delete);
        }
        for (MemberDevice device : createdDevices) {
            memberDeviceRepository.findById(device.getId()).ifPresent(memberDeviceRepository::delete);
        }
        memberRepository.deleteAll(createdMembers);
        noticeRepository.deleteAll(createdNotices);
        noticeDetailsRepository.deleteAll(createdNoticeDetails);
        for (DepartmentType departmentType : createdDepartmentTypes) {
            departmentRepository.findById(departmentType).ifPresent(departmentRepository::delete);
        }
    }

    private void saveDepartment(DepartmentType departmentType, String name) {
        departmentRepository.save(new Department(departmentType, name, null));
        createdDepartmentTypes.add(departmentType);
    }

    private void saveNotice(DepartmentType departmentType, Long noticeDetailsId, String title) {
        Department department = departmentRepository.getReferenceById(departmentType);
        NoticeDetails details = noticeDetailsRepository.save(
                new NoticeDetails(noticeDetailsId, "작성자", title, "/view/" + noticeDetailsId,
                        LocalDate.of(2026, 8, 20)));
        createdNoticeDetails.add(details);

        createdNotices.add(noticeRepository.save(new Notice(department, details)));
    }

    private MemberDevice saveGuestDevice(String token) {
        MemberDevice device = MemberDevice.builder()
                .deviceToken(token)
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();

        MemberDevice saved = memberDeviceRepository.save(device);
        createdDevices.add(saved);

        return saved;
    }

    @Test
    @DisplayName("여러 학과를 구독한 비회원은 구독한 모든 학과와 대학 공지를 함께 받는다")
    void guest_with_multiple_subscriptions_gets_notices_from_all_of_them() throws Exception {
        MemberDevice device = saveGuestDevice("token-home-multi");
        preferenceRepository.save(new DeviceNoticePreference(device,
                departmentRepository.getReferenceById(DepartmentType.DEPT_2001)));
        preferenceRepository.save(new DeviceNoticePreference(device,
                departmentRepository.getReferenceById(DepartmentType.DEPT_3001)));

        mockMvc.perform(get("/home")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, device.getDeviceToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices.length()").value(3))
                .andExpect(jsonPath("$.notices[*].title")
                        .value(Matchers.containsInAnyOrder("대학 공지 1", "컴공 공지 1", "기계 공지 1")));
    }

    @Test
    @DisplayName("구독한 학과가 없는 비회원은 기본 홈 화면(대학 공지만)으로 폴백한다")
    void guest_with_no_subscriptions_falls_back_to_default_home() throws Exception {
        MemberDevice device = saveGuestDevice("token-home-empty");

        mockMvc.perform(get("/home")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, device.getDeviceToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices.length()").value(1))
                .andExpect(jsonPath("$.notices[0].title").value("대학 공지 1"));
    }

    @Test
    @DisplayName("디바이스 토큰 헤더가 없으면 기본 홈 화면으로 폴백한다")
    void missing_header_falls_back_to_default_home() throws Exception {
        mockMvc.perform(get("/home")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices.length()").value(1))
                .andExpect(jsonPath("$.notices[0].title").value("대학 공지 1"));
    }

    @Test
    @DisplayName("알 수 없는 디바이스 토큰은 오류 없이 기본 홈 화면으로 폴백한다")
    void unknown_key_falls_back_to_default_home() throws Exception {
        mockMvc.perform(get("/home")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, "no-such-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices.length()").value(1))
                .andExpect(jsonPath("$.notices[0].title").value("대학 공지 1"));
    }

    @Test
    @DisplayName("회원에 바인딩된 디바이스의 토큰은 기본 홈 화면으로 폴백한다")
    void member_bound_device_key_falls_back_to_default_home() throws Exception {
        Department department = departmentRepository.getReferenceById(DepartmentType.DEPT_2001);
        Member member = memberRepository.save(Member.builder()
                .email("bound-home@dongyang.ac.kr")
                .nickname("바인드홈")
                .password("encoded")
                .department(department)
                .build());
        createdMembers.add(member);

        MemberDevice device = saveGuestDevice("token-home-bound");
        preferenceRepository.save(new DeviceNoticePreference(device, department));
        device.bindMember(member);
        memberDeviceRepository.save(device);

        mockMvc.perform(get("/home")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, device.getDeviceToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices.length()").value(1))
                .andExpect(jsonPath("$.notices[0].title").value("대학 공지 1"));
    }

    @Test
    @DisplayName("구독 학과가 겹쳐 3개 이상 공지가 대상이 되어도 최신 3개만 반환한다")
    void truncates_to_top_3_most_recent_when_more_than_3_are_eligible() throws Exception {
        saveDepartment(DepartmentType.DEPT_4001, "자동화공학과");
        saveDepartment(DepartmentType.DEPT_5001, "전기공학과");

        saveNotice(DepartmentType.DEPT_4001, 90101L, "자동화 공지 구버전");
        saveNotice(DepartmentType.DEPT_4001, 90102L, "자동화 공지 신버전");
        saveNotice(DepartmentType.DEPT_5001, 90103L, "전기 공지 구버전");
        saveNotice(DepartmentType.DEPT_5001, 90104L, "전기 공지 최신");

        MemberDevice device = saveGuestDevice("token-home-truncate");
        preferenceRepository.save(new DeviceNoticePreference(device,
                departmentRepository.getReferenceById(DepartmentType.DEPT_4001)));
        preferenceRepository.save(new DeviceNoticePreference(device,
                departmentRepository.getReferenceById(DepartmentType.DEPT_5001)));

        // 대상 공지는 대학 공지(90001) + 자동화 2건 + 전기 2건 = 5건이지만, id 내림차순 상위 3건만 와야 한다
        mockMvc.perform(get("/home")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, device.getDeviceToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices.length()").value(3))
                .andExpect(jsonPath("$.notices[*].title")
                        .value(Matchers.containsInAnyOrder("전기 공지 최신", "전기 공지 구버전", "자동화 공지 신버전")));
    }

    @Test
    @DisplayName("대상 공지가 3개 미만이면 있는 만큼만 반환하고 채워 넣지 않는다")
    void returns_all_eligible_notices_without_padding_when_fewer_than_3_exist() throws Exception {
        saveDepartment(DepartmentType.DEPT_6001, "생명화학공학과");
        saveNotice(DepartmentType.DEPT_6001, 90105L, "생명화학 공지 1");

        MemberDevice device = saveGuestDevice("token-home-fewer-than-3");
        preferenceRepository.save(new DeviceNoticePreference(device,
                departmentRepository.getReferenceById(DepartmentType.DEPT_6001)));

        mockMvc.perform(get("/home")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, device.getDeviceToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices.length()").value(2))
                .andExpect(jsonPath("$.notices[*].title")
                        .value(Matchers.containsInAnyOrder("대학 공지 1", "생명화학 공지 1")));
    }

    @Test
    @DisplayName("대학 공지(DEPT_1001) 자체를 구독해도 중복 없이 한 번만 반환된다")
    void subscribing_to_official_department_does_not_duplicate_its_notice() throws Exception {
        saveDepartment(DepartmentType.DEPT_7001, "경영학과");
        saveNotice(DepartmentType.DEPT_7001, 90106L, "경영 공지 1");

        MemberDevice device = saveGuestDevice("token-home-dept1001-subscribed");
        // 대학 공지 학과(DEPT_1001)를 직접 구독하는 것이 현재 정책상 막혀있지 않음을 함께 확인한다
        preferenceRepository.save(new DeviceNoticePreference(device,
                departmentRepository.getReferenceById(DepartmentType.DEPT_1001)));
        preferenceRepository.save(new DeviceNoticePreference(device,
                departmentRepository.getReferenceById(DepartmentType.DEPT_7001)));

        mockMvc.perform(get("/home")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, device.getDeviceToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices.length()").value(2))
                .andExpect(jsonPath("$.notices[*].title")
                        .value(Matchers.containsInAnyOrder("대학 공지 1", "경영 공지 1")));
    }

    @Test
    @DisplayName("구독 학과가 10개 이상이어도 오류 없이 최신 3개를 정확히 반환한다")
    void handles_large_subscription_set_without_error() throws Exception {
        List<DepartmentType> manyDepartments = List.of(
                DepartmentType.DEPT_2002, DepartmentType.DEPT_2003, DepartmentType.DEPT_3002,
                DepartmentType.DEPT_4002, DepartmentType.DEPT_5002, DepartmentType.DEPT_5003,
                DepartmentType.DEPT_5004, DepartmentType.DEPT_6002, DepartmentType.DEPT_6003,
                DepartmentType.DEPT_6004);
        for (DepartmentType departmentType : manyDepartments) {
            saveDepartment(departmentType, departmentType.name());
        }

        saveNotice(DepartmentType.DEPT_6002, 90107L, "대량구독 공지 1");
        saveNotice(DepartmentType.DEPT_6003, 90108L, "대량구독 공지 2");
        saveNotice(DepartmentType.DEPT_6004, 90109L, "대량구독 공지 3 (최신)");

        MemberDevice device = saveGuestDevice("token-home-many-subscriptions");
        for (DepartmentType departmentType : manyDepartments) {
            preferenceRepository.save(new DeviceNoticePreference(device,
                    departmentRepository.getReferenceById(departmentType)));
        }

        // 대상 공지는 대학 공지(90001) + 3건 = 4건이므로 최신 3건(90109, 90108, 90107)만 반환되어야 한다
        mockMvc.perform(get("/home")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, device.getDeviceToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices.length()").value(3))
                .andExpect(jsonPath("$.notices[*].title")
                        .value(Matchers.containsInAnyOrder(
                                "대량구독 공지 3 (최신)", "대량구독 공지 2", "대량구독 공지 1")));
    }

    @Test
    @DisplayName("[알려진 비대칭] 여러 학과를 구독해도 추천 모집글은 구독한 학과 중 하나만 반영한다 (공지와 달리 다학과 미지원, 의도된 동작)")
    void recruitment_section_only_reflects_a_single_subscribed_department_by_design() throws Exception {
        saveDepartment(DepartmentType.DEPT_8001, "자유전공학과");
        saveDepartment(DepartmentType.DEPT_9001, "교양과");

        HomeRecruitment recruitmentForDept8001 = new HomeRecruitment(
                1L, 0L, "자유전공 모집글", "내용", "태그", RecruitmentType.PROJECT.name());
        HomeRecruitment recruitmentForDept9001 = new HomeRecruitment(
                2L, 0L, "교양 모집글", "내용", "태그", RecruitmentType.PROJECT.name());
        BDDMockito.given(recruitmentRepository.searchHomeRecruitment(DepartmentType.DEPT_8001.name()))
                .willReturn(List.of(recruitmentForDept8001));
        BDDMockito.given(recruitmentRepository.searchHomeRecruitment(DepartmentType.DEPT_9001.name()))
                .willReturn(List.of(recruitmentForDept9001));

        MemberDevice device = saveGuestDevice("token-home-recruitment-asymmetry");
        preferenceRepository.save(new DeviceNoticePreference(device,
                departmentRepository.getReferenceById(DepartmentType.DEPT_8001)));
        preferenceRepository.save(new DeviceNoticePreference(device,
                departmentRepository.getReferenceById(DepartmentType.DEPT_9001)));

        // 두 학과 모두 구독했지만, 추천 모집글은 둘 중 하나(첫 구독 학과)만 나오고 합쳐지지 않아야 한다
        mockMvc.perform(get("/home")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header(DEVICE_TOKEN_HEADER, device.getDeviceToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popular_recruitments.length()").value(1))
                .andExpect(jsonPath("$.popular_recruitments[0].title")
                        .value(Matchers.isIn(List.of("자유전공 모집글", "교양 모집글"))));
    }

    @Test
    @DisplayName("회원용 홈 화면(GET /home/{departmentType})은 비회원 다학과 개인화 변경의 영향을 받지 않는다")
    void member_home_endpoint_is_unaffected_by_guest_multi_department_personalization() throws Exception {
        Department department = departmentRepository.getReferenceById(DepartmentType.DEPT_3001);
        Member member = memberRepository.save(Member.builder()
                .email("home-member-smoke@dongyang.ac.kr")
                .nickname("멤버홈스모크")
                .password("encoded")
                .department(department)
                .build());
        createdMembers.add(member);

        String accessToken = testJwtTokenGenerator.generateAccessToken(member.getId());

        mockMvc.perform(get("/home/{departmentType}", DepartmentType.DEPT_3001)
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notices[*].title")
                        .value(Matchers.containsInAnyOrder("대학 공지 1", "기계 공지 1")));
    }
}
