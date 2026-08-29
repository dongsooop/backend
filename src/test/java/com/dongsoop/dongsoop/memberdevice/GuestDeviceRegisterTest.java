package com.dongsoop.dongsoop.memberdevice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.exception.AlreadyRegisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
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
class GuestDeviceRegisterTest {

    @Autowired
    private MemberDeviceService memberDeviceService;

    @Autowired
    private MemberDeviceRepository memberDeviceRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @MockitoBean
    private FCMService fcmService;

    @MockitoBean
    private BoardSearchRepository boardSearchRepository;

    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    @Test
    @DisplayName("처음 등록하는 비회원 디바이스는 새 행으로 저장된다")
    void creates_new_row_for_brand_new_guest_device() {
        memberDeviceService.registerDevice("token-new", MemberDeviceType.ANDROID, null);

        assertThat(memberDeviceRepository.findByDeviceToken("token-new")).isPresent();
    }

    @Test
    @DisplayName("이미 등록된 토큰으로 다시 등록해도 새 행을 만들지 않는다")
    void does_not_duplicate_row_for_already_registered_token() {
        memberDeviceService.registerDevice("token-existing", MemberDeviceType.ANDROID, null);
        long countBefore = memberDeviceRepository.count();

        memberDeviceService.registerDevice("token-existing", MemberDeviceType.ANDROID, null);

        assertThat(memberDeviceRepository.count()).isEqualTo(countBefore);
    }

    @Test
    @DisplayName("회원 소유 디바이스 토큰으로 미인증 재등록하면 예외를 던진다")
    void rejects_reregistration_of_member_owned_device() {
        departmentRepository.save(new Department(DepartmentType.DEPT_2001, "컴퓨터소프트웨어공학과", "https://example.test/notice"));
        Department department = departmentRepository.getReferenceById(DepartmentType.DEPT_2001);
        Member member = memberRepository.save(Member.builder()
                .email("guest-test@dongyang.ac.kr")
                .nickname("테스트원")
                .password("encoded")
                .department(department)
                .build());

        MemberDevice owned = MemberDevice.builder()
                .deviceToken("token-owned")
                .memberDeviceType(MemberDeviceType.ANDROID)
                .member(member)
                .build();
        memberDeviceRepository.save(owned);

        assertThatThrownBy(() ->
                memberDeviceService.registerDevice("token-owned", MemberDeviceType.ANDROID, null))
                .isInstanceOf(AlreadyRegisteredDeviceException.class);
    }
}
