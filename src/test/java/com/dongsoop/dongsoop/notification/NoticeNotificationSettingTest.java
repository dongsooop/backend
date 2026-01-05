package com.dongsoop.dongsoop.notification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.dongsoop.dongsoop.TestDataSourceConfig;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.notification.NoticeNotificationImpl;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestDataSourceConfig.class)
public class NoticeNotificationSettingTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @Autowired
    NoticeNotificationImpl noticeNotification;

    @Autowired
    NotificationSettingRepository notificationSettingRepository;

    @Autowired
    MemberDeviceRepository memberDeviceRepository;

    @MockitoBean
    NotificationSendService notificationSendService;
    @MockitoBean
    FCMService fcmService;   // 외부 연동 차단
    @MockitoBean
    private BoardSearchRepository boardSearchRepository;
    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    @BeforeEach
    void setup() {
        Department department1 = departmentRepository.save(new Department(DepartmentType.DEPT_2001, "학과명1", null));
        Department department2 = departmentRepository.save(new Department(DepartmentType.DEPT_3001, "학과명2", null));

        Member member1 = memberRepository.save(
                new Member(null, "test1@dongyang.ac.kr", "이름1", "password", null, department1));
        Member member2 = memberRepository.save(
                new Member(null, "test2@dongyang.ac.kr", "이름2", "password", null, department1));
        Member member3 = memberRepository.save(
                new Member(null, "test3@dongyang.ac.kr", "이름3", "password", null, department2));

        MemberDevice memberDevice1 = memberDeviceRepository.save(
                new MemberDevice(null, member1, "token1", MemberDeviceType.IOS));
        MemberDevice memberDevice2 = memberDeviceRepository.save(
                new MemberDevice(null, member2, "token2", MemberDeviceType.WEB));
        MemberDevice memberDevice3 = memberDeviceRepository.save(
                new MemberDevice(null, member3, "token3", MemberDeviceType.ANDROID));

        NotificationSetting notificationSetting = new NotificationSetting(memberDevice2, NotificationType.NOTICE,
                false);

        notificationSettingRepository.save(notificationSetting); // member2는 공지 알림 수신 거부
    }

    @Test
    @DisplayName("공지 알림 수신 거부한 회원은 알림 대상에서 제외되어야 한다")
    void sendNotification_WhenMemberDisabledNotice_ExcludeFromTargetList() {
        // given
        ArgumentCaptor<List> captor =
                ArgumentCaptor.forClass(java.util.List.class);

        Set<Notice> noticeDetailSet = new HashSet<>();

        Department department = new Department(DepartmentType.DEPT_2001, "학과명", null);
        NoticeDetails noticeDetails = new NoticeDetails();
        Notice notice = new Notice(department, noticeDetails);

        noticeDetailSet.add(notice);

        // when
        noticeNotification.send(noticeDetailSet);

        // notificationSendService.sendAll이 호출되었고 전달된 리스트를 검사
        verify(notificationSendService)
                .sendAll(captor.capture(), any());
        java.util.List<?> memberNotificationList = captor.getValue();

        // then
        assertNotNull(memberNotificationList);
        assertFalse(memberNotificationList.isEmpty());
        Assertions.assertEquals(1, memberNotificationList.size(), "member1이 포함되어야 합니다.");
        assertTrue(
                memberNotificationList.stream().anyMatch(e -> {
                    MemberNotification mn = (MemberNotification) e;
                    return Objects.equals(mn.getId().getMember().getId(), 1L);
                }),
                "member1이 포함되어야 합니다.");
    }
}
