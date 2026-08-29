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
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
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
class ExpiredGuestDeviceCleanupTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @Autowired
    private DeviceNoticePreferenceRepository preferenceRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    /**
     * 비회원 기기는 lastAccess 가 등록 시점에 고정되고, FCM 토큰이 만료되면 deviceToken 이 null 이 된다.
     * 즉 학과를 설정한 기기가 정리 대상이 되는 상황은 예외가 아니라 정상 경로다.
     */
    @Test
    @DisplayName("학과를 설정한 비회원 기기도 만료 정리에서 함께 삭제된다")
    void deletes_expired_guest_device_with_its_preferences() {
        departmentRepository.save(new Department(DepartmentType.DEPT_2001, "컴퓨터소프트웨어공학과",
                "https://example.test/notice"));
        Department department = departmentRepository.getReferenceById(DepartmentType.DEPT_2001);

        MemberDevice device = MemberDevice.builder()
                .deviceToken(null)
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();
        device.issueAnonymousKeyIfAbsent();
        memberDeviceRepository.save(device);
        device.updateLastAccess(LocalDateTime.now().minusDays(30));

        preferenceRepository.save(new DeviceNoticePreference(device, department));
        memberDeviceRepository.flush();

        long deleted = memberDeviceRepository.deleteExpiredDevices(LocalDateTime.now().minusDays(1));

        // 벌크 삭제는 영속성 컨텍스트를 우회하므로, 캐시된 엔티티가 아니라 DB 상태를 확인한다
        entityManager.clear();

        assertThat(deleted).isEqualTo(1);
        assertThat(memberDeviceRepository.findById(device.getId())).isEmpty();
        assertThat(preferenceRepository.findAllByIdDeviceId(device.getId())).isEmpty();
    }
}
