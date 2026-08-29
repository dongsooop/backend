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

    private MemberDevice saveGuest(String token, DepartmentType departmentType) {
        return saveGuest(token, departmentType, MemberDeviceType.ANDROID);
    }

    private MemberDevice saveGuest(String token, DepartmentType departmentType, MemberDeviceType deviceType) {
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
    @DisplayName("학과가 일치하는 비회원만 조회된다")
    void finds_only_matching_department() {
        MemberDevice matched = saveGuest("token-q-1", DepartmentType.DEPT_2001);
        saveGuest("token-q-2", DepartmentType.DEPT_3001);

        List<MemberDevice> result = memberDeviceRepository.searchGuestDevicesByDepartment(DepartmentType.DEPT_2001);

        assertThat(result).extracting(MemberDevice::getId).containsExactly(matched.getId());
    }

    @Test
    @DisplayName("대학 공지는 학과 미설정 비회원까지 포함한다")
    void university_notice_includes_all_guests() {
        MemberDevice withDept = saveGuest("token-q-3", DepartmentType.DEPT_2001);
        MemberDevice withoutDept = saveGuest("token-q-4", null);

        List<MemberDevice> result = memberDeviceRepository.searchGuestDevicesByDepartment(DepartmentType.DEPT_1001);

        assertThat(result).extracting(MemberDevice::getId)
                .contains(withDept.getId(), withoutDept.getId());
    }

    @Test
    @DisplayName("NOTICE 알림을 끈 비회원은 제외된다")
    void excludes_device_with_notice_disabled() {
        MemberDevice device = saveGuest("token-q-5", DepartmentType.DEPT_2001);
        notificationSettingRepository.save(new NotificationSetting(device, NotificationType.NOTICE, false));

        List<MemberDevice> result = memberDeviceRepository.searchGuestDevicesByDepartment(DepartmentType.DEPT_2001);

        assertThat(result).extracting(MemberDevice::getId).doesNotContain(device.getId());
    }

    @Test
    @DisplayName("NotificationSetting 행이 없으면 NOTICE 기본 활성 상태(true)를 따라 포함된다")
    void falls_back_to_default_enabled_state_when_no_notification_setting_row() {
        // NOTICE.getDefaultActiveState() == true 이므로, 설정 행이 없는 기기는 활성 취급되어 포함되어야 한다
        MemberDevice device = saveGuest("token-q-6", DepartmentType.DEPT_2001);

        List<MemberDevice> result = memberDeviceRepository.searchGuestDevicesByDepartment(DepartmentType.DEPT_2001);

        assertThat(result).extracting(MemberDevice::getId).contains(device.getId());
    }

    @Test
    @DisplayName("WEB 타입 비회원 기기는 푸시 대상에서 제외된다")
    void excludes_web_type_guest_device() {
        MemberDevice webDevice = saveGuest("token-q-7", DepartmentType.DEPT_2001, MemberDeviceType.WEB);

        List<MemberDevice> result = memberDeviceRepository.searchGuestDevicesByDepartment(DepartmentType.DEPT_2001);

        assertThat(result).extracting(MemberDevice::getId).doesNotContain(webDevice.getId());
    }

    @Test
    @DisplayName("비회원으로 학과를 구독했던 기기가 회원으로 전환되면 더 이상 조회되지 않는다 (중복 발송 방지의 핵심 속성)")
    void member_bound_device_no_longer_appears_in_guest_search() {
        Department department = departmentRepository.getReferenceById(DepartmentType.DEPT_2001);
        MemberDevice device = saveGuest("token-q-8", DepartmentType.DEPT_2001);

        assertThat(memberDeviceRepository.searchGuestDevicesByDepartment(DepartmentType.DEPT_2001))
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

        assertThat(memberDeviceRepository.searchGuestDevicesByDepartment(DepartmentType.DEPT_2001))
                .extracting(MemberDevice::getId)
                .doesNotContain(device.getId());
    }
}
