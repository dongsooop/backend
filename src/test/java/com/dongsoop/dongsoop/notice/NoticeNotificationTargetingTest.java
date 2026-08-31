package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.dto.DeviceSubscription;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.keyword.service.NoticeKeywordFilter;
import com.dongsoop.dongsoop.notice.keyword.service.NoticeKeywordService;
import com.dongsoop.dongsoop.notice.notification.NoticeNotificationImpl;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 공지 알림 타겟팅은 회원/비회원 구분 없이 device_notice_preference 구독 여부로만 결정된다.
 * (Member.department 기반 자동 타겟팅은 제거됨)
 */
@ExtendWith(MockitoExtension.class)
class NoticeNotificationTargetingTest {

    @Mock
    private NotificationSaveService notificationSaveService;

    @Mock
    private NotificationSendService notificationSendService;

    @Mock
    private MemberDeviceRepository memberDeviceRepository;

    @Mock
    private NoticeKeywordService noticeKeywordService;

    @InjectMocks
    private NoticeNotificationImpl noticeNotification;

    private Notice buildNotice(DepartmentType departmentType) {
        Department department = new Department(departmentType, "컴퓨터소프트웨어공학과",
                "https://example.test/notice");
        // NoticeDetails 는 @AllArgsConstructor 를 갖는다: (id, writer, title, link, createdAt)
        NoticeDetails details = new NoticeDetails(1L, "학사지원팀", "장학금 안내", "/view/1", LocalDate.of(2026, 8, 26));

        return new Notice(department, details);
    }

    private Notice buildNotice() {
        return buildNotice(DepartmentType.DEPT_2001);
    }

    private Member buildMember(Long id, DepartmentType departmentType) {
        Department department = new Department(departmentType, "학과명", null);
        return Member.builder()
                .id(id)
                .email("member" + id + "@dongyang.ac.kr")
                .nickname("회원" + id)
                .password("encoded")
                .department(department)
                .build();
    }

    // 키워드 필터가 기기 id 로 설정을 찾으므로, 영속화된 엔티티처럼 id 를 부여한다
    private long deviceIdSequence = 1L;

    private MemberDevice buildDevice(String token, Member member) {
        MemberDevice device = MemberDevice.builder()
                .id(deviceIdSequence++)
                .deviceToken(token)
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();
        if (member != null) {
            device.bindMember(member);
        }
        return device;
    }

    /**
     * 기기 조회는 대상 학과 전체를 한 번에 받고, 학과별 분배는 구독 쌍으로 이뤄진다.
     * 대학 공지는 구독 쌍이 없어도 전체 기기가 대상이므로 subscriptions 를 비워 둔다.
     */
    private void givenDevices(List<MemberDevice> devices, DeviceSubscription... subscriptions) {
        given(memberDeviceRepository.searchDevicesByDepartments(anyCollection())).willReturn(devices);
        given(memberDeviceRepository.findSubscriptionsByDeviceIds(anyCollection()))
                .willReturn(List.of(subscriptions));
    }

    private DeviceSubscription subscribe(MemberDevice device, DepartmentType departmentType) {
        return new DeviceSubscription(device.getId(), departmentType);
    }

    @Test
    @DisplayName("device_notice_preference를 구독한 회원 기기는 알림함 저장 및 발송 대상이 된다")
    void member_device_with_preference_receives_notice() {
        ReflectionTestUtils.setField(noticeNotification, "universityDomain", "https://example.test");
        Notice notice = buildNotice(DepartmentType.DEPT_2001);
        Member member = buildMember(1L, DepartmentType.DEPT_2001);
        MemberDevice device = buildDevice("member-token", member);

        givenDevices(List.of(device), subscribe(device, DepartmentType.DEPT_2001));
        given(noticeKeywordService.loadFilter(anyList())).willReturn(new NoticeKeywordFilter(Map.of()));
        given(notificationSaveService.saveAll(anyList(), any(), any(), any(), any())).willReturn(List.of());

        noticeNotification.send(Set.of(notice));

        ArgumentCaptor<List<Member>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationSaveService).saveAll(membersCaptor.capture(), any(), any(), any(), any());
        assertThat(membersCaptor.getValue()).containsExactly(member);
    }

    @Test
    @DisplayName("Member.department는 X이지만 device_notice_preference 구독이 없는 회원 기기는 더 이상 알림 대상이 아니다")
    void member_without_preference_row_does_not_receive_notice_even_if_department_matches() {
        ReflectionTestUtils.setField(noticeNotification, "universityDomain", "https://example.test");
        Notice notice = buildNotice(DepartmentType.DEPT_2001);

        // Member.department == DEPT_2001 이지만 device_notice_preference 행이 없으므로
        // searchDevicesByDepartment 쿼리 결과에 포함되지 않는다 (레포지토리가 구독 여부로만 필터링)
        givenDevices(List.of());
        given(noticeKeywordService.loadFilter(anyList())).willReturn(new NoticeKeywordFilter(Map.of()));
        given(notificationSaveService.saveAll(anyList(), any(), any(), any(), any())).willReturn(List.of());

        noticeNotification.send(Set.of(notice));

        ArgumentCaptor<List<Member>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationSaveService).saveAll(membersCaptor.capture(), any(), any(), any(), any());
        assertThat(membersCaptor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("비회원 디바이스가 있으면 푸시를 직접 전송한다")
    void sends_push_to_guest_devices() {
        ReflectionTestUtils.setField(noticeNotification, "universityDomain", "https://example.test");
        Notice notice = buildNotice();

        MemberDevice guest = buildDevice("guest-token", null);

        givenDevices(List.of(guest), subscribe(guest, DepartmentType.DEPT_2001));
        given(noticeKeywordService.loadFilter(anyList())).willReturn(new NoticeKeywordFilter(Map.of()));
        given(notificationSaveService.saveAll(anyList(), any(), any(), any(), any())).willReturn(List.of());

        noticeNotification.send(Set.of(notice));

        verify(notificationSendService).send(eq(List.of("guest-token")), any(NotificationSend.class));
    }

    @Test
    @DisplayName("한 조회 결과 안에서 회원 기기와 비회원 기기는 단순 boolean 분할이므로 서로 중복되지 않는다")
    void member_and_guest_devices_are_mutually_exclusive_partition() {
        ReflectionTestUtils.setField(noticeNotification, "universityDomain", "https://example.test");
        Notice notice = buildNotice();
        Member member = buildMember(1L, DepartmentType.DEPT_2001);
        MemberDevice memberDevice = buildDevice("member-token", member);
        MemberDevice guestDevice = buildDevice("guest-token", null);

        givenDevices(List.of(memberDevice, guestDevice),
                subscribe(memberDevice, DepartmentType.DEPT_2001),
                subscribe(guestDevice, DepartmentType.DEPT_2001));
        given(noticeKeywordService.loadFilter(anyList())).willReturn(new NoticeKeywordFilter(Map.of()));
        given(notificationSaveService.saveAll(anyList(), any(), any(), any(), any())).willReturn(List.of());

        noticeNotification.send(Set.of(notice));

        ArgumentCaptor<List<Member>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationSaveService).saveAll(membersCaptor.capture(), any(), any(), any(), any());
        assertThat(membersCaptor.getValue()).containsExactly(member);

        verify(notificationSendService).send(eq(List.of("guest-token")), any(NotificationSend.class));
    }

    @Test
    @DisplayName("대학 공지(전체 학과)는 구독 행 없이도 회원/비회원 기기 모두에 도달한다")
    void university_wide_notice_reaches_member_and_guest_without_preference() {
        ReflectionTestUtils.setField(noticeNotification, "universityDomain", "https://example.test");
        Notice notice = buildNotice(DepartmentType.DEPT_1001);
        Member member = buildMember(1L, DepartmentType.DEPT_2001);
        MemberDevice memberDevice = buildDevice("member-token", member);
        MemberDevice guestDevice = buildDevice("guest-token", null);

        givenDevices(List.of(memberDevice, guestDevice));
        given(noticeKeywordService.loadFilter(anyList())).willReturn(new NoticeKeywordFilter(Map.of()));
        given(notificationSaveService.saveAll(anyList(), any(), any(), any(), any())).willReturn(List.of());

        noticeNotification.send(Set.of(notice));

        ArgumentCaptor<List<Member>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationSaveService).saveAll(membersCaptor.capture(), any(), any(), any(), any());
        assertThat(membersCaptor.getValue()).containsExactly(member);

        verify(notificationSendService).send(eq(List.of("guest-token")), any(NotificationSend.class));
    }

    @Test
    @DisplayName("WEB 타입이거나 NOTICE 알림이 꺼진 기기는 레포지토리 조회 단계에서 이미 제외되므로 알림 대상에 없다")
    void excluded_device_never_reaches_notification_layer() {
        ReflectionTestUtils.setField(noticeNotification, "universityDomain", "https://example.test");
        Notice notice = buildNotice();

        // 레포지토리가 isNotWebDevice()/NOTICE 알림 설정 조건으로 이미 걸러내므로
        // searchDevicesByDepartment는 빈 목록을 반환한다
        givenDevices(List.of());
        given(noticeKeywordService.loadFilter(anyList())).willReturn(new NoticeKeywordFilter(Map.of()));
        given(notificationSaveService.saveAll(anyList(), any(), any(), any(), any())).willReturn(List.of());

        noticeNotification.send(Set.of(notice));

        ArgumentCaptor<List<Member>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationSaveService).saveAll(membersCaptor.capture(), any(), any(), any(), any());
        assertThat(membersCaptor.getValue()).isEmpty();
        verify(notificationSendService, never()).send(anyList(), any(NotificationSend.class));
    }

    @Test
    @DisplayName("공지가 여러 건이어도 각 공지는 그 학과를 구독한 기기로만 발송된다")
    void notices_do_not_share_target_devices() {
        ReflectionTestUtils.setField(noticeNotification, "universityDomain", "https://example.test");
        Notice csNotice = buildNotice(DepartmentType.DEPT_2001);
        Notice aiNotice = buildNotice(DepartmentType.DEPT_2002);

        // 같은 회원이 두 학과를 서로 다른 기기로 구독한 상황
        Member member = buildMember(1L, DepartmentType.DEPT_2001);
        MemberDevice csDevice = buildDevice("cs-token", member);
        MemberDevice aiDevice = buildDevice("ai-token", member);

        givenDevices(List.of(csDevice, aiDevice),
                subscribe(csDevice, DepartmentType.DEPT_2001),
                subscribe(aiDevice, DepartmentType.DEPT_2002));
        given(noticeKeywordService.loadFilter(anyList())).willReturn(new NoticeKeywordFilter(Map.of()));
        given(notificationSaveService.saveAll(anyList(), any(), any(), any(), any())).willReturn(List.of());

        noticeNotification.send(Set.of(csNotice, aiNotice));

        // 공지별 대상을 합쳐서 회원 단위로 보내면 컴퓨터소프트웨어 공지가 ai-token 으로도 나간다
        ArgumentCaptor<List<MemberDevice>> devicesCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationSendService, times(2)).sendAllToDevices(anyList(), devicesCaptor.capture());
        assertThat(devicesCaptor.getAllValues())
                .containsExactlyInAnyOrder(List.of(csDevice), List.of(aiDevice));
    }

    @Test
    @DisplayName("비회원 대상이 500명을 넘으면 FCM 멀티캐스트 한도만큼 나눠서 전송한다")
    void chunks_guest_push_when_over_fcm_multicast_limit() {
        ReflectionTestUtils.setField(noticeNotification, "universityDomain", "https://example.test");
        Notice notice = buildNotice();

        List<MemberDevice> guests = IntStream.range(0, 501)
                .<MemberDevice>mapToObj(i -> buildDevice("guest-token-" + i, null))
                .toList();

        givenDevices(guests, guests.stream()
                .map(guest -> subscribe(guest, DepartmentType.DEPT_2001))
                .toArray(DeviceSubscription[]::new));
        given(noticeKeywordService.loadFilter(anyList())).willReturn(new NoticeKeywordFilter(Map.of()));
        given(notificationSaveService.saveAll(anyList(), any(), any(), any(), any())).willReturn(List.of());

        noticeNotification.send(Set.of(notice));

        ArgumentCaptor<List<String>> tokensCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationSendService, times(2)).send(tokensCaptor.capture(), any(NotificationSend.class));

        List<List<String>> capturedChunks = tokensCaptor.getAllValues();
        assertThat(capturedChunks).hasSize(2);
        assertThat(capturedChunks.get(0)).hasSize(500);
        assertThat(capturedChunks.get(1)).hasSize(1);
    }
}
