package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.keyword.service.NoticeKeywordService;
import com.dongsoop.dongsoop.notice.notification.NoticeNotificationImpl;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.time.LocalDate;
import java.util.List;
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

    private MemberDevice buildDevice(String token, Member member) {
        MemberDevice device = MemberDevice.builder()
                .deviceToken(token)
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();
        if (member != null) {
            device.bindMember(member);
        }
        return device;
    }

    @Test
    @DisplayName("device_notice_preference를 구독한 회원 기기는 알림함 저장 및 발송 대상이 된다")
    void member_device_with_preference_receives_notice() {
        ReflectionTestUtils.setField(noticeNotification, "universityDomain", "https://example.test");
        Notice notice = buildNotice(DepartmentType.DEPT_2001);
        Member member = buildMember(1L, DepartmentType.DEPT_2001);
        MemberDevice device = buildDevice("member-token", member);

        given(memberDeviceRepository.searchDevicesByDepartment(DepartmentType.DEPT_2001))
                .willReturn(List.of(device));
        given(noticeKeywordService.filterMembersByKeyword(anyList(), any()))
                .willAnswer(invocation -> invocation.getArgument(0));
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
        given(memberDeviceRepository.searchDevicesByDepartment(DepartmentType.DEPT_2001))
                .willReturn(List.of());
        given(noticeKeywordService.filterMembersByKeyword(anyList(), any()))
                .willAnswer(invocation -> invocation.getArgument(0));
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

        given(memberDeviceRepository.searchDevicesByDepartment(DepartmentType.DEPT_2001))
                .willReturn(List.of(guest));
        given(noticeKeywordService.filterMembersByKeyword(anyList(), any()))
                .willAnswer(invocation -> invocation.getArgument(0));
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

        given(memberDeviceRepository.searchDevicesByDepartment(DepartmentType.DEPT_2001))
                .willReturn(List.of(memberDevice, guestDevice));
        given(noticeKeywordService.filterMembersByKeyword(anyList(), any()))
                .willAnswer(invocation -> invocation.getArgument(0));
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

        given(memberDeviceRepository.searchDevicesByDepartment(DepartmentType.DEPT_1001))
                .willReturn(List.of(memberDevice, guestDevice));
        given(noticeKeywordService.filterMembersByKeyword(anyList(), any()))
                .willAnswer(invocation -> invocation.getArgument(0));
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
        given(memberDeviceRepository.searchDevicesByDepartment(DepartmentType.DEPT_2001))
                .willReturn(List.of());
        given(noticeKeywordService.filterMembersByKeyword(anyList(), any()))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(notificationSaveService.saveAll(anyList(), any(), any(), any(), any())).willReturn(List.of());

        noticeNotification.send(Set.of(notice));

        ArgumentCaptor<List<Member>> membersCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationSaveService).saveAll(membersCaptor.capture(), any(), any(), any(), any());
        assertThat(membersCaptor.getValue()).isEmpty();
        verify(notificationSendService, never()).send(anyList(), any(NotificationSend.class));
    }

    @Test
    @DisplayName("비회원 대상이 500명을 넘으면 FCM 멀티캐스트 한도만큼 나눠서 전송한다")
    void chunks_guest_push_when_over_fcm_multicast_limit() {
        ReflectionTestUtils.setField(noticeNotification, "universityDomain", "https://example.test");
        Notice notice = buildNotice();

        List<MemberDevice> guests = IntStream.range(0, 501)
                .<MemberDevice>mapToObj(i -> buildDevice("guest-token-" + i, null))
                .toList();

        given(memberDeviceRepository.searchDevicesByDepartment(DepartmentType.DEPT_2001))
                .willReturn(guests);
        given(noticeKeywordService.filterMembersByKeyword(anyList(), any()))
                .willAnswer(invocation -> invocation.getArgument(0));
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
