package com.dongsoop.dongsoop.memberdevice;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.preference.entity.DeviceNoticePreference;
import com.dongsoop.dongsoop.notice.preference.repository.DeviceNoticePreferenceRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code MemberDeviceRepositoryCustomImpl.searchDevicesByDepartment}는 회원/비회원을
 * 구분하지 않고 device_notice_preference 구독 여부로만 대상을 조회한다. 회원/비회원 분리는
 * 이 쿼리가 아니라 {@code NoticeNotificationImpl.send()}의 발송 단계에서 이루어진다
 * ({@link com.dongsoop.dongsoop.notice.NoticeNotificationTargetingTest} 참고).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GuestDeviceQueryTest {

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @Autowired
    private DeviceNoticePreferenceRepository preferenceRepository;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    @BeforeEach
    void seedDepartments() {
        departmentRepository.save(new Department(DepartmentType.DEPT_2001, "학과명1", null));
        departmentRepository.save(new Department(DepartmentType.DEPT_3001, "학과명2", null));
    }

    private MemberDevice saveDevice(String token, DepartmentType departmentType) {
        return saveDevice(token, departmentType, MemberDeviceType.ANDROID);
    }

    private MemberDevice saveDevice(String token, DepartmentType departmentType, MemberDeviceType deviceType) {
        MemberDevice device = MemberDevice.builder()
                .deviceToken(token)
                .memberDeviceType(deviceType)
                .build();
        memberDeviceRepository.save(device);

        if (departmentType != null) {
            Department department = departmentRepository.getReferenceById(departmentType);
            preferenceRepository.save(new DeviceNoticePreference(device, department));
        }

        return device;
    }

    @Test
    @DisplayName("학과가 일치하는 디바이스만 조회된다")
    void finds_only_matching_department() {
        MemberDevice matched = saveDevice("token-q-1", DepartmentType.DEPT_2001);
        saveDevice("token-q-2", DepartmentType.DEPT_3001);

        List<MemberDevice> result = memberDeviceRepository.searchDevicesByDepartments(List.of(DepartmentType.DEPT_2001));

        assertThat(result).extracting(MemberDevice::getId).containsExactly(matched.getId());
    }

    @Test
    @DisplayName("대학 공지는 학과 구독이 없는 디바이스까지 포함한다")
    void university_notice_includes_all_devices() {
        MemberDevice withDept = saveDevice("token-q-3", DepartmentType.DEPT_2001);
        MemberDevice withoutDept = saveDevice("token-q-4", null);

        List<MemberDevice> result = memberDeviceRepository.searchDevicesByDepartments(List.of(DepartmentType.DEPT_1001));

        assertThat(result).extracting(MemberDevice::getId)
                .contains(withDept.getId(), withoutDept.getId());
    }

    @Test
    @DisplayName("NOTICE 알림을 끈 디바이스는 제외된다")
    void excludes_device_with_notice_disabled() {
        MemberDevice device = saveDevice("token-q-5", DepartmentType.DEPT_2001);
        notificationSettingRepository.save(new NotificationSetting(device, NotificationType.NOTICE, false));

        List<MemberDevice> result = memberDeviceRepository.searchDevicesByDepartments(List.of(DepartmentType.DEPT_2001));

        assertThat(result).extracting(MemberDevice::getId).doesNotContain(device.getId());
    }

    @Test
    @DisplayName("NotificationSetting 행이 없으면 NOTICE 기본 활성 상태(true)를 따라 포함된다")
    void falls_back_to_default_enabled_state_when_no_notification_setting_row() {
        // NOTICE.getDefaultActiveState() == true 이므로, 설정 행이 없는 기기는 활성 취급되어 포함되어야 한다
        MemberDevice device = saveDevice("token-q-6", DepartmentType.DEPT_2001);

        List<MemberDevice> result = memberDeviceRepository.searchDevicesByDepartments(List.of(DepartmentType.DEPT_2001));

        assertThat(result).extracting(MemberDevice::getId).contains(device.getId());
    }

    @Test
    @DisplayName("WEB 타입 기기는 푸시 대상에서 제외된다")
    void excludes_web_type_device() {
        MemberDevice webDevice = saveDevice("token-q-7", DepartmentType.DEPT_2001, MemberDeviceType.WEB);

        List<MemberDevice> result = memberDeviceRepository.searchDevicesByDepartments(List.of(DepartmentType.DEPT_2001));

        assertThat(result).extracting(MemberDevice::getId).doesNotContain(webDevice.getId());
    }

    @Test
    @DisplayName("구독해둔 기기가 회원으로 전환돼도 계속 조회된다 (회원/비회원 분리는 발송 단계에서 처리)")
    void device_still_appears_after_binding_to_member() {
        Department department = departmentRepository.getReferenceById(DepartmentType.DEPT_2001);
        MemberDevice device = saveDevice("token-q-8", DepartmentType.DEPT_2001);

        assertThat(memberDeviceRepository.searchDevicesByDepartments(List.of(DepartmentType.DEPT_2001)))
                .extracting(MemberDevice::getId)
                .contains(device.getId());

        Member member = memberRepository.save(Member.builder()
                .email("converted@dongyang.ac.kr")
                .nickname("전환회원")
                .password("encoded")
                .department(department)
                .build());
        device.bindMember(member);
        memberDeviceRepository.save(device);

        // 이 쿼리 결과에서는 계속 포함된다 — 이제 이 디바이스는 회원 소유이므로,
        // NoticeNotificationImpl.send()가 이 결과를 회원/비회원으로 나눌 때 회원 쪽(알림함 저장) 경로로 간다.
        assertThat(memberDeviceRepository.searchDevicesByDepartments(List.of(DepartmentType.DEPT_2001)))
                .extracting(MemberDevice::getId)
                .contains(device.getId());
    }
}
