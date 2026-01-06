package com.dongsoop.dongsoop.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.TestDataSourceConfig;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.department.service.DepartmentService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notice.dto.CrawledNotice;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.Notice.NoticeKey;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.notification.NoticeNotificationImpl;
import com.dongsoop.dongsoop.notice.service.NoticeScheduler;
import com.dongsoop.dongsoop.notice.service.NoticeService;
import com.dongsoop.dongsoop.notice.util.NoticeCrawl;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import com.dongsoop.dongsoop.search.repository.BoardSearchRepository;
import com.dongsoop.dongsoop.search.repository.RestaurantSearchRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestDataSourceConfig.class)
public class NoticeNotificationSettingTest {

    private static final Logger log = LoggerFactory.getLogger(NoticeNotificationSettingTest.class);

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    NoticeScheduler noticeScheduler;

    @MockitoSpyBean
    MemberDeviceService memberDeviceService;

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

    @Autowired
    NotificationSendService notificationSendService;

    @MockitoBean
    NoticeCrawl noticeCrawl;

    @MockitoBean
    NoticeService noticeService;

    @MockitoBean
    DepartmentService departmentService;

    @MockitoBean
    FCMService fcmService;   // 외부 연동 차단
    @MockitoBean
    private BoardSearchRepository boardSearchRepository;
    @MockitoBean
    private RestaurantSearchRepository restaurantSearchRepository;

    private Department department1;
    private Department department2;

    private Member member1;
    private Member member2;
    private Member member3;

    @BeforeEach
    void setup() {
        jdbc.execute("TRUNCATE member RESTART IDENTITY CASCADE");

        department1 = departmentRepository.save(new Department(DepartmentType.DEPT_2001, "학과명1", null));
        department2 = departmentRepository.save(new Department(DepartmentType.DEPT_3001, "학과명2", null));

        member1 = memberRepository.save(
                new Member(null, "test1@dongyang.ac.kr", "이름1", "password", null, department1));
        member2 = memberRepository.save(
                new Member(null, "test2@dongyang.ac.kr", "이름2", "password", null, department1));
        member3 = memberRepository.save(
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
        LocalDate.now();
        NoticeDetails noticeDetails = new NoticeDetails(null, "writer", "title", "link", LocalDate.now());
        Notice notice = new Notice(new NoticeKey(department1, noticeDetails));

        when(noticeService.getNoticeRecentIdMap())
                .thenReturn(Map.of(department1, 0L, department2, 0L));
        when(departmentService.getAllDepartments())
                .thenReturn(List.of(department1, department2));
        // DEPT_2001 학과는 공지 1개 조회
        when(noticeCrawl.crawlNewNotices(department1, 0L))
                .thenReturn(new CrawledNotice(Set.of(noticeDetails), List.of(notice)));
        when(noticeCrawl.crawlNewNotices(department2, 0L))
                .thenReturn(new CrawledNotice(Set.of(), List.of()));

        Set<Notice> noticeDetailSet = new HashSet<>();

        Department department = new Department(DepartmentType.DEPT_2001, "학과명", null);

        noticeDetailSet.add(notice);

        AtomicReference<Map<Long, List<String>>> captured = new AtomicReference<>();

        Mockito.doAnswer(invocation -> {
            Map<Long, List<String>> result = (Map<Long, List<String>>) invocation.callRealMethod();
            captured.set(result);
            return result;
        }).when(memberDeviceService).getDeviceByMember(any());

        // when
        noticeScheduler.scheduled();

        // then
        assertNotNull(captured);

        Map<Long, List<String>> capturedMap = captured.get();

        assertNotNull(capturedMap);
        assertFalse(capturedMap.isEmpty());
        assertEquals(capturedMap.size(), 1,
                "member1만 포함된 1개 요소여야 합니다: [" + capturedMap.keySet().stream()
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(",")) + "]");
        assertTrue(capturedMap.containsKey(member1.getId()),
                "member1만 포함되어야 합니다.");
    }
}
