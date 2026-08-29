package com.dongsoop.dongsoop.notice.preference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.preference.repository.DeviceNoticePreferenceRepository;
import com.dongsoop.dongsoop.notice.preference.service.GuestNoticePreferenceService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GuestDepartmentTest {

    @Autowired
    private GuestNoticePreferenceService service;

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @MockitoSpyBean
    private DeviceNoticePreferenceRepository preferenceRepository;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    @BeforeEach
    void seedDepartments() {
        // H2 테스트 프로필에는 학과 데이터가 없어 직접 저장한다.
        departmentRepository.save(new Department(DepartmentType.DEPT_2001, "컴퓨터소프트웨어공학과", null));
        departmentRepository.save(new Department(DepartmentType.DEPT_3001, "기계공학과", null));
        departmentRepository.save(new Department(DepartmentType.DEPT_4001, "자동화공학과", null));
        departmentRepository.save(new Department(DepartmentType.DEPT_4002, "로봇소프트웨어과", null));
    }

    private void seedAllDepartments() {
        Arrays.stream(DepartmentType.values())
                .forEach(type -> departmentRepository.save(new Department(type, type.getName(), null)));
    }

    private MemberDevice saveGuestDevice(String token) {
        MemberDevice device = MemberDevice.builder()
                .deviceToken(token)
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();

        return memberDeviceRepository.save(device);
    }

    @Test
    @DisplayName("비회원이 학과를 설정하면 조회 시 같은 학과가 나온다")
    void sets_and_reads_department() {
        MemberDevice device = saveGuestDevice("token-dept-1");

        service.updateDepartments(device.getDeviceToken(), Set.of(DepartmentType.DEPT_2001));

        assertThat(service.getDepartments(device.getDeviceToken()).departmentTypes())
                .containsExactly(DepartmentType.DEPT_2001);
    }

    @Test
    @DisplayName("비회원은 여러 학과를 동시에 구독할 수 있다")
    void subscribes_to_multiple_departments() {
        MemberDevice device = saveGuestDevice("token-dept-multi");

        service.updateDepartments(device.getDeviceToken(), Set.of(DepartmentType.DEPT_2001, DepartmentType.DEPT_3001));

        assertThat(service.getDepartments(device.getDeviceToken()).departmentTypes())
                .containsExactlyInAnyOrder(DepartmentType.DEPT_2001, DepartmentType.DEPT_3001);
    }

    @Test
    @DisplayName("학과 목록을 다시 설정하면 전체 교체된다 (빠진 학과는 삭제, 새 학과는 추가)")
    void replaces_department_set() {
        MemberDevice device = saveGuestDevice("token-dept-2");

        service.updateDepartments(device.getDeviceToken(), Set.of(DepartmentType.DEPT_2001));
        service.updateDepartments(device.getDeviceToken(), Set.of(DepartmentType.DEPT_3001));

        assertThat(service.getDepartments(device.getDeviceToken()).departmentTypes())
                .containsExactly(DepartmentType.DEPT_3001);
    }

    @Test
    @DisplayName("학과를 설정하지 않은 비회원은 빈 목록을 반환한다")
    void returns_empty_when_not_set() {
        MemberDevice device = saveGuestDevice("token-dept-3");

        assertThat(service.getDepartments(device.getDeviceToken()).departmentTypes()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 디바이스 토큰은 거부한다")
    void rejects_unknown_token() {
        assertThatThrownBy(() -> service.getDepartments("no-such-token"))
                .isInstanceOf(UnregisteredDeviceException.class);
    }

    @Test
    @DisplayName("회원에 바인딩된 디바이스의 토큰은 거부한다")
    void rejects_member_bound_device() {
        Department department = departmentRepository.getReferenceById(DepartmentType.DEPT_2001);
        Member member = memberRepository.save(Member.builder()
                .email("bound@dongyang.ac.kr")
                .nickname("바인드")
                .password("encoded")
                .department(department)
                .build());

        MemberDevice device = saveGuestDevice("token-dept-4");
        device.bindMember(member);
        memberDeviceRepository.save(device);

        assertThatThrownBy(() -> service.getDepartments(device.getDeviceToken()))
                .isInstanceOf(UnregisteredDeviceException.class);
    }

    @Test
    @DisplayName("완전히 겹치지 않는 학과 집합으로 교체하면 이전 구독은 모두 사라지고 새 집합만 남는다")
    void replaces_with_completely_disjoint_multi_department_set() {
        MemberDevice device = saveGuestDevice("token-dept-disjoint");

        service.updateDepartments(device.getDeviceToken(), Set.of(DepartmentType.DEPT_2001, DepartmentType.DEPT_3001));
        service.updateDepartments(device.getDeviceToken(), Set.of(DepartmentType.DEPT_4001, DepartmentType.DEPT_4002));

        assertThat(service.getDepartments(device.getDeviceToken()).departmentTypes())
                .containsExactlyInAnyOrder(DepartmentType.DEPT_4001, DepartmentType.DEPT_4002);
    }

    @Test
    @DisplayName("같은 학과 집합을 다시 제출하면 불필요한 삭제/재삽입 없이 그대로 유지된다")
    void no_op_when_same_department_set_is_resubmitted() {
        MemberDevice device = saveGuestDevice("token-dept-noop");
        Set<DepartmentType> sameSet = Set.of(DepartmentType.DEPT_2001, DepartmentType.DEPT_3001);

        service.updateDepartments(device.getDeviceToken(), sameSet);
        Mockito.clearInvocations(preferenceRepository);

        service.updateDepartments(device.getDeviceToken(), sameSet);

        Mockito.verify(preferenceRepository, Mockito.never()).deleteAll(Mockito.anyList());
        Mockito.verify(preferenceRepository, Mockito.never()).saveAll(Mockito.anyList());
        assertThat(service.getDepartments(device.getDeviceToken()).departmentTypes())
                .containsExactlyInAnyOrder(DepartmentType.DEPT_2001, DepartmentType.DEPT_3001);
    }

    @Test
    @DisplayName("빈 집합으로 교체하면 전체 구독 해지가 된다 (PUT API로도 도달 가능, GuestEndpointSecurityTest 참고)")
    void unsubscribing_all_via_empty_set_is_allowed() {
        MemberDevice device = saveGuestDevice("token-dept-unsub-all");
        service.updateDepartments(device.getDeviceToken(), Set.of(DepartmentType.DEPT_2001, DepartmentType.DEPT_3001));

        service.updateDepartments(device.getDeviceToken(), Set.of());

        assertThat(service.getDepartments(device.getDeviceToken()).departmentTypes()).isEmpty();
    }

    @Test
    @DisplayName("실존하는 모든 학과(약 26개)를 한 번에 구독할 수 있다")
    void subscribes_to_every_real_department_at_once() {
        seedAllDepartments();
        MemberDevice device = saveGuestDevice("token-dept-all");
        Set<DepartmentType> allDepartments = Arrays.stream(DepartmentType.values()).collect(Collectors.toSet());

        service.updateDepartments(device.getDeviceToken(), allDepartments);

        assertThat(service.getDepartments(device.getDeviceToken()).departmentTypes())
                .containsExactlyInAnyOrderElementsOf(allDepartments);
    }
}
