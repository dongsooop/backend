package com.dongsoop.dongsoop.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import com.dongsoop.dongsoop.calendar.entity.OfficialSchedule;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepository;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepository;
import com.dongsoop.dongsoop.calendar.schedule.CalendarScheduler;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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
public class CalendarNotificationSettingTest extends AbstractIntegrationTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    NotificationSettingRepository notificationSettingRepository;

    @Autowired
    MemberDeviceRepository memberDeviceRepository;

    @Autowired
    DepartmentRepository departmentRepository;

    @MockitoSpyBean
    MemberDeviceService memberDeviceService;

    @Autowired
    CalendarScheduler calendarScheduler;

    @Autowired
    MemberScheduleRepository memberScheduleRepository;

    @Autowired
    OfficialScheduleRepository officialScheduleRepository;

    @MockitoBean
    FCMService fcmService;   // 외부 연동 차단
    @MockitoBean
    private BoardSearchRepository boardSearchRepository;
    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    private Member member1;
    private Member member2;
    private Member member3;

    @BeforeEach
    void setUp() {
        // 학과 정보 저장
        Department department1 = departmentRepository.save(new Department(DepartmentType.DEPT_2001, "학과명1", null));
        Department department2 = departmentRepository.save(new Department(DepartmentType.DEPT_3001, "학과명2", null));

        // 회원 정보 저장
        member1 = memberRepository.save(
                new Member(null, "test1@dongyang.ac.kr", "이름1", "password", null, department1));
        member2 = memberRepository.save(
                new Member(null, "test2@dongyang.ac.kr", "이름2", "password", null, department1));
        member3 = memberRepository.save(
                new Member(null, "test3@dongyang.ac.kr", "이름3", "password", null, department2));

        // 디바이스 정보 저장
        MemberDevice memberDevice1 = memberDeviceRepository.save(
                new MemberDevice(null, member1, "token1", MemberDeviceType.IOS));
        MemberDevice memberDevice2 = memberDeviceRepository.save(
                new MemberDevice(null, member2, "token2", MemberDeviceType.WEB));
        MemberDevice memberDevice3 = memberDeviceRepository.save(
                new MemberDevice(null, member3, "token3", MemberDeviceType.ANDROID));

        // 회원 일정 추가
        MemberSchedule memberSchedule1 = new MemberSchedule(null, "title", "content", LocalDateTime.now(),
                LocalDateTime.now(), member1);
        MemberSchedule memberSchedule2 = new MemberSchedule(null, "title", "content", LocalDateTime.now(),
                LocalDateTime.now(), member2);
        MemberSchedule memberSchedule3 = new MemberSchedule(null, "title", "content", LocalDateTime.now(),
                LocalDateTime.now(), member3);

        memberScheduleRepository.saveAll(List.of(memberSchedule1, memberSchedule2, memberSchedule3));

        // 공식 일정 추가
        OfficialSchedule officialSchedule = new OfficialSchedule(null, "title", LocalDate.now(), LocalDate.now());
        officialScheduleRepository.save(officialSchedule);

        NotificationSetting notificationSetting = new NotificationSetting(memberDevice1, NotificationType.CALENDAR,
                false);
        notificationSettingRepository.save(notificationSetting); // member1은 공지 알림 수신 거부
    }

    @Test
    @DisplayName("")
    void sendCalendarNotification_WhenMemberDisabledCalendarNotification_ThenNotSendNotification() {
        // given
        AtomicReference<Map<Long, List<String>>> captured = new AtomicReference<>();

        Mockito.doAnswer(invocation -> {
            Map<Long, List<String>> result = (Map<Long, List<String>>) invocation.callRealMethod();
            captured.set(result);
            return result;
        }).when(memberDeviceService).getDeviceByMember(any());

        // when
        calendarScheduler.send();

        // then
        Map<Long, List<String>> capturedMap = captured.get();

        assertNotNull(capturedMap);
        assertFalse(capturedMap.isEmpty());
        assertEquals(capturedMap.size(), 2,
                "member2, member3를 포함한 2개 요소여야 합니다: [" + capturedMap.keySet().stream()
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(",")) + "]");
        assertTrue(capturedMap.keySet().containsAll(List.of(member2.getId(), member3.getId())),
                "member2, member3이 포함되어야 합니다.");
    }
}
