package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.dto.NoticeListResponse;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.preference.service.GuestNoticePreferenceService;
import com.dongsoop.dongsoop.notice.repository.NoticeDetailsRepository;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.notice.service.NoticeService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GuestNoticeListTest {

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private GuestNoticePreferenceService guestNoticePreferenceService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NoticeDetailsRepository noticeDetailsRepository;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    private Department deptA;
    private Department deptB;
    private Department deptC;

    @BeforeEach
    void seedDepartments() {
        // H2 테스트 프로필에는 학과 데이터가 없어 직접 저장한다.
        deptA = departmentRepository.save(new Department(DepartmentType.DEPT_2001, "컴퓨터소프트웨어공학과", null));
        deptB = departmentRepository.save(new Department(DepartmentType.DEPT_3001, "기계공학과", null));
        deptC = departmentRepository.save(new Department(DepartmentType.DEPT_4001, "자동화공학과", null));
    }

    private MemberDevice saveGuestDevice(String token) {
        MemberDevice device = MemberDevice.builder()
                .deviceToken(token)
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();

        return memberDeviceRepository.save(device);
    }

    private void saveNotice(Department department, long noticeId, String title, LocalDate createdAt) {
        NoticeDetails details = noticeDetailsRepository.save(
                new NoticeDetails(noticeId, "학사지원팀", title, "/view/" + noticeId, createdAt));
        noticeRepository.save(new Notice(department, details));
    }

    @Test
    @DisplayName("여러 학과를 구독한 비회원은 구독한 학과들의 공지만 모아서 조회한다")
    void returns_combined_notices_from_subscribed_departments() {
        MemberDevice device = saveGuestDevice("token-list-1");
        guestNoticePreferenceService.updateDepartments(device.getDeviceToken(), Set.of(deptA.getId(), deptB.getId()));

        saveNotice(deptA, 1L, "A 학과 공지", LocalDate.of(2026, 8, 1));
        saveNotice(deptB, 2L, "B 학과 공지", LocalDate.of(2026, 8, 2));
        saveNotice(deptC, 3L, "구독하지 않은 C 학과 공지", LocalDate.of(2026, 8, 3));

        Page<NoticeListResponse> result = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(NoticeListResponse::getId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("구독한 학과가 없는 비회원은 빈 페이지를 받는다")
    void returns_empty_page_when_no_subscriptions() {
        MemberDevice device = saveGuestDevice("token-list-2");
        saveNotice(deptA, 4L, "A 학과 공지", LocalDate.of(2026, 8, 1));

        Page<NoticeListResponse> result = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("디바이스 토큰이 없으면 에러 없이 빈 페이지를 받는다")
    void returns_empty_page_when_key_missing() {
        Page<NoticeListResponse> result = noticeService.getNoticeForGuest(null, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 디바이스 토큰은 에러 없이 빈 페이지를 받는다")
    void returns_empty_page_when_key_unknown() {
        Page<NoticeListResponse> result = noticeService.getNoticeForGuest("no-such-key", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("회원에 바인딩된 디바이스의 토큰은 에러 없이 빈 페이지를 받는다")
    void returns_empty_page_when_device_bound_to_member() {
        Department department = departmentRepository.getReferenceById(DepartmentType.DEPT_2001);
        Member member = memberRepository.save(Member.builder()
                .email("bound-list@dongyang.ac.kr")
                .nickname("바인드리스트")
                .password("encoded")
                .department(department)
                .build());

        MemberDevice device = saveGuestDevice("token-list-3");
        device.bindMember(member);
        memberDeviceRepository.save(device);

        Page<NoticeListResponse> result = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("여러 학과의 공지를 페이지 크기에 맞춰 나눠 받는다")
    void paginates_across_multiple_departments() {
        MemberDevice device = saveGuestDevice("token-list-4");
        guestNoticePreferenceService.updateDepartments(device.getDeviceToken(), Set.of(deptA.getId(), deptB.getId()));

        saveNotice(deptA, 10L, "A 공지 1", LocalDate.of(2026, 8, 10));
        saveNotice(deptA, 11L, "A 공지 2", LocalDate.of(2026, 8, 11));
        saveNotice(deptB, 12L, "B 공지 1", LocalDate.of(2026, 8, 12));

        Page<NoticeListResponse> firstPage = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(0, 2));
        Page<NoticeListResponse> secondPage = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(1, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("여러 학과에 걸쳐 3페이지 이상 나뉘어도 각 페이지가 정확히 분할되고 누락/중복이 없다")
    void paginates_correctly_across_three_or_more_pages() {
        MemberDevice device = saveGuestDevice("token-list-5");
        guestNoticePreferenceService.updateDepartments(device.getDeviceToken(), Set.of(deptA.getId(), deptB.getId()));

        // id 오름차순 = 등록 순, 내림차순이 최신순이 되도록 20~24 순서로 저장
        saveNotice(deptA, 20L, "A 공지 20", LocalDate.of(2026, 8, 20));
        saveNotice(deptB, 21L, "B 공지 21", LocalDate.of(2026, 8, 21));
        saveNotice(deptA, 22L, "A 공지 22", LocalDate.of(2026, 8, 22));
        saveNotice(deptB, 23L, "B 공지 23", LocalDate.of(2026, 8, 23));
        saveNotice(deptA, 24L, "A 공지 24", LocalDate.of(2026, 8, 24));

        Page<NoticeListResponse> page0 = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(0, 2));
        Page<NoticeListResponse> page1 = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(1, 2));
        Page<NoticeListResponse> page2 = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(2, 2));

        assertThat(page0.getTotalElements()).isEqualTo(5);
        assertThat(page0.getTotalPages()).isEqualTo(3);

        assertThat(page0.getContent()).extracting(NoticeListResponse::getId).containsExactly(24L, 23L);
        assertThat(page1.getContent()).extracting(NoticeListResponse::getId).containsExactly(22L, 21L);
        assertThat(page2.getContent()).extracting(NoticeListResponse::getId).containsExactly(20L);

        List<Long> allIds = Stream.of(page0, page1, page2)
                .flatMap(page -> page.getContent().stream())
                .map(NoticeListResponse::getId)
                .toList();
        assertThat(allIds).doesNotHaveDuplicates();
        assertThat(allIds).containsExactlyInAnyOrder(20L, 21L, 22L, 23L, 24L);
    }

    @Test
    @DisplayName("여러 학과에 뒤섞여 저장된 공지도 학과 구분 없이 최신순으로 통합 정렬된다")
    void sorts_combined_result_newest_first_across_departments() {
        MemberDevice device = saveGuestDevice("token-list-6");
        guestNoticePreferenceService.updateDepartments(device.getDeviceToken(),
                Set.of(deptA.getId(), deptB.getId(), deptC.getId()));

        // 저장 순서를 뒤섞어도 결과는 id(=최신순) 내림차순이어야 한다
        saveNotice(deptC, 33L, "C 공지 33", LocalDate.of(2026, 8, 13));
        saveNotice(deptA, 31L, "A 공지 31", LocalDate.of(2026, 8, 11));
        saveNotice(deptB, 35L, "B 공지 35", LocalDate.of(2026, 8, 15));
        saveNotice(deptA, 32L, "A 공지 32", LocalDate.of(2026, 8, 12));
        saveNotice(deptB, 34L, "B 공지 34", LocalDate.of(2026, 8, 14));

        Page<NoticeListResponse> result = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(NoticeListResponse::getId)
                .containsExactly(35L, 34L, 33L, 32L, 31L);
    }

    @Test
    @DisplayName("구독하지 않은 학과의 공지는 페이지 경계에서도 절대 노출되지 않는다")
    void excludes_unsubscribed_department_notices_at_page_boundary() {
        MemberDevice device = saveGuestDevice("token-list-7");
        guestNoticePreferenceService.updateDepartments(device.getDeviceToken(), Set.of(deptA.getId()));

        saveNotice(deptA, 40L, "A 공지 40", LocalDate.of(2026, 8, 10));
        saveNotice(deptA, 41L, "A 공지 41", LocalDate.of(2026, 8, 11));
        // deptB, deptC는 구독하지 않음 - 결과에 절대 포함되면 안 된다
        saveNotice(deptB, 42L, "구독 안 한 B 공지", LocalDate.of(2026, 8, 12));
        saveNotice(deptC, 43L, "구독 안 한 C 공지", LocalDate.of(2026, 8, 13));

        Page<NoticeListResponse> firstPage = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(0, 1));
        Page<NoticeListResponse> secondPage = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(1, 1));

        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        List<Long> allIds = Stream.of(firstPage, secondPage)
                .flatMap(page -> page.getContent().stream())
                .map(NoticeListResponse::getId)
                .toList();
        assertThat(allIds).containsExactlyInAnyOrder(40L, 41L);
        assertThat(allIds).doesNotContain(42L, 43L);
    }

    @Test
    @DisplayName("전체 대학 공지(DEPT_1001)를 구독하지 않았다면 비회원 통합 목록에서 제외된다")
    void excludes_university_wide_official_department_notice() {
        Department officialDepartment = departmentRepository.save(
                new Department(DepartmentType.DEPT_1001, "동양미래대학", null));

        MemberDevice device = saveGuestDevice("token-list-8");
        guestNoticePreferenceService.updateDepartments(device.getDeviceToken(), Set.of(deptA.getId()));

        saveNotice(deptA, 50L, "A 학과 공지", LocalDate.of(2026, 8, 10));
        saveNotice(officialDepartment, 51L, "전체 대학 공지", LocalDate.of(2026, 8, 11));

        Page<NoticeListResponse> result = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(NoticeListResponse::getId)
                .containsExactly(50L);
        assertThat(result.getContent())
                .extracting(NoticeListResponse::getId)
                .doesNotContain(51L);
    }

    @Test
    @DisplayName("페이지 크기가 전체 공지 수보다 크면 에러 없이 0페이지에 전부 담긴다")
    void returns_all_notices_on_first_page_when_page_size_exceeds_total() {
        MemberDevice device = saveGuestDevice("token-list-9");
        guestNoticePreferenceService.updateDepartments(device.getDeviceToken(), Set.of(deptA.getId(), deptB.getId()));

        saveNotice(deptA, 60L, "A 공지 60", LocalDate.of(2026, 8, 10));
        saveNotice(deptB, 61L, "B 공지 61", LocalDate.of(2026, 8, 11));

        Page<NoticeListResponse> result = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(0, 100));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("마지막 페이지를 넘어선 페이지 번호를 요청하면 에러 없이 빈 페이지를 받는다")
    void returns_empty_page_when_requesting_beyond_last_page() {
        MemberDevice device = saveGuestDevice("token-list-10");
        guestNoticePreferenceService.updateDepartments(device.getDeviceToken(), Set.of(deptA.getId()));

        saveNotice(deptA, 70L, "A 공지 70", LocalDate.of(2026, 8, 10));

        Page<NoticeListResponse> result = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(5, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("탈퇴/삭제되어 존재하지 않는 기기 토큰도 미등록 토큰과 동일하게 에러 없이 빈 페이지를 받는다")
    void returns_empty_page_when_device_no_longer_exists() {
        // 별도의 소프트 삭제 플래그가 없으므로, 저장된 적 없는 토큰과 동일하게 취급되는지 확인한다
        Page<NoticeListResponse> result = noticeService.getNoticeForGuest("token-never-registered-or-deleted",
                PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("비회원 통합 목록의 응답 필드 구성은 단일 학과 공지 조회(/notice/{departmentType})와 동일하다")
    void response_shape_matches_single_department_endpoint() {
        MemberDevice device = saveGuestDevice("token-list-11");
        guestNoticePreferenceService.updateDepartments(device.getDeviceToken(), Set.of(deptA.getId()));

        saveNotice(deptA, 80L, "A 공지 80", LocalDate.of(2026, 8, 10));

        Page<NoticeListResponse> guestResult = noticeService.getNoticeForGuest(device.getDeviceToken(),
                PageRequest.of(0, 10));
        Page<NoticeListResponse> singleDepartmentResult = noticeService.getNoticeByDepartmentType(
                DepartmentType.DEPT_2001, PageRequest.of(0, 10));

        NoticeListResponse guestNotice = guestResult.getContent().get(0);
        NoticeListResponse singleDepartmentNotice = singleDepartmentResult.getContent().get(0);

        assertThat(guestNotice.getId()).isEqualTo(singleDepartmentNotice.getId());
        assertThat(guestNotice.getWriter()).isEqualTo(singleDepartmentNotice.getWriter());
        assertThat(guestNotice.getTitle()).isEqualTo(singleDepartmentNotice.getTitle());
        assertThat(guestNotice.getLink()).isEqualTo(singleDepartmentNotice.getLink());
        assertThat(guestNotice.getCreatedAt()).isEqualTo(singleDepartmentNotice.getCreatedAt());
    }
}
