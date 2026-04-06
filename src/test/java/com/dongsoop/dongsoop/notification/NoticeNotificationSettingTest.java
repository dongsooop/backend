package com.dongsoop.dongsoop.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import java.util.List;
import java.util.Map;
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
public class NoticeNotificationSettingTest extends AbstractIntegrationTest {

    @Autowired
    MemberDeviceService memberDeviceService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    NotificationSettingRepository notificationSettingRepository;

    @Autowired
    MemberDeviceRepository memberDeviceRepository;

    @MockitoBean
    FCMService fcmService;
    @MockitoBean
    private BoardSearchRepository boardSearchRepository;
    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    private Department department1;
    private Member member1;
    private Member member2;

    @BeforeEach
    void setup() {
        department1 = departmentRepository.save(new Department(DepartmentType.DEPT_2001, "학과명1", null));
        Department department2 = departmentRepository.save(new Department(DepartmentType.DEPT_3001, "학과명2", null));

        member1 = memberRepository.save(
                new Member(null, "test1@dongyang.ac.kr", "이름1", "password", null, department1));
        member2 = memberRepository.save(
                new Member(null, "test2@dongyang.ac.kr", "이름2", "password", null, department1));
        Member member3 = memberRepository.save(
                new Member(null, "test3@dongyang.ac.kr", "이름3", "password", null, department2));

        memberDeviceRepository.save(
                MemberDevice.builder().member(member1).deviceToken("token1").memberDeviceType(MemberDeviceType.IOS)
                        .build());
        MemberDevice memberDevice2 = memberDeviceRepository.save(
                MemberDevice.builder().member(member2).deviceToken("token2").memberDeviceType(MemberDeviceType.WEB)
                        .build());
        memberDeviceRepository.save(
                MemberDevice.builder().member(member3).deviceToken("token3").memberDeviceType(MemberDeviceType.ANDROID)
                        .build());

        NotificationSetting notificationSetting = new NotificationSetting(memberDevice2, NotificationType.NOTICE,
                false);

        notificationSettingRepository.save(notificationSetting); // member2는 WEB 타입 + 공지 알림 수신 거부
    }

    @Test
    @DisplayName("공지 알림 수신 거부한 회원은 알림 대상에서 제외되어야 한다")
    void sendNotification_WhenMemberDisabledNotice_ExcludeFromTargetList() {
        // given - member1, member2가 department1 소속으로 알림 대상
        MemberDeviceFindCondition condition = new MemberDeviceFindCondition(
                List.of(member1.getId(), member2.getId()),
                NotificationType.NOTICE
        );

        // when
        Map<Long, List<String>> result = memberDeviceService.getDeviceByMember(condition);

        // then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size(),
                "member1만 포함된 1개 요소여야 합니다: [" + result.keySet().stream()
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(",")) + "]");
        assertTrue(result.containsKey(member1.getId()),
                "member1만 포함되어야 합니다.");
    }
}
