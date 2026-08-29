package com.dongsoop.dongsoop.memberdevice;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
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
        MemberDevice device = MemberDevice.builder()
                .deviceToken(token)
                .memberDeviceType(MemberDeviceType.ANDROID)
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
}
