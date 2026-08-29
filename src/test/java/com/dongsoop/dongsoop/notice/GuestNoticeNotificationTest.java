package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
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

@ExtendWith(MockitoExtension.class)
class GuestNoticeNotificationTest {

    @Mock
    private NotificationSaveService notificationSaveService;

    @Mock
    private NotificationSendService notificationSendService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberDeviceRepository memberDeviceRepository;

    @Mock
    private NoticeKeywordService noticeKeywordService;

    @InjectMocks
    private NoticeNotificationImpl noticeNotification;

    private Notice buildNotice() {
        Department department = new Department(DepartmentType.DEPT_2001, "컴퓨터소프트웨어공학과",
                "https://example.test/notice");
        // NoticeDetails 는 @AllArgsConstructor 를 갖는다: (id, writer, title, link, createdAt)
        NoticeDetails details = new NoticeDetails(1L, "학사지원팀", "장학금 안내", "/view/1", LocalDate.of(2026, 8, 26));

        return new Notice(department, details);
    }

    @Test
    @DisplayName("비회원 디바이스가 있으면 푸시를 직접 전송한다")
    void sends_push_to_guest_devices() {
        ReflectionTestUtils.setField(noticeNotification, "universityDomain", "https://example.test");
        Notice notice = buildNotice();

        MemberDevice guest = MemberDevice.builder()
                .deviceToken("guest-token")
                .memberDeviceType(MemberDeviceType.ANDROID)
                .build();

        given(memberRepository.searchAllByDepartmentAndDeviceNotEmpty(any())).willReturn(List.of());
        given(noticeKeywordService.filterMembersByKeyword(anyList(), any())).willReturn(List.of());
        given(notificationSaveService.saveAll(anyList(), any(), any(), any(), any())).willReturn(List.of());
        given(memberDeviceRepository.searchGuestDevicesByDepartment(DepartmentType.DEPT_2001))
                .willReturn(List.of(guest));

        noticeNotification.send(Set.of(notice));

        verify(notificationSendService).send(eq(List.of("guest-token")), any(NotificationSend.class));
    }

    @Test
    @DisplayName("비회원 대상이 500명을 넘으면 FCM 멀티캐스트 한도만큼 나눠서 전송한다")
    void chunks_guest_push_when_over_fcm_multicast_limit() {
        ReflectionTestUtils.setField(noticeNotification, "universityDomain", "https://example.test");
        Notice notice = buildNotice();

        List<MemberDevice> guests = IntStream.range(0, 501)
                .<MemberDevice>mapToObj(i -> MemberDevice.builder()
                        .deviceToken("guest-token-" + i)
                        .memberDeviceType(MemberDeviceType.ANDROID)
                        .build())
                .toList();

        given(memberRepository.searchAllByDepartmentAndDeviceNotEmpty(any())).willReturn(List.of());
        given(noticeKeywordService.filterMembersByKeyword(anyList(), any())).willReturn(List.of());
        given(notificationSaveService.saveAll(anyList(), any(), any(), any(), any())).willReturn(List.of());
        given(memberDeviceRepository.searchGuestDevicesByDepartment(DepartmentType.DEPT_2001))
                .willReturn(guests);

        noticeNotification.send(Set.of(notice));

        ArgumentCaptor<List<String>> tokensCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationSendService, times(2)).send(tokensCaptor.capture(), any(NotificationSend.class));

        List<List<String>> capturedChunks = tokensCaptor.getAllValues();
        assertThat(capturedChunks).hasSize(2);
        assertThat(capturedChunks.get(0)).hasSize(500);
        assertThat(capturedChunks.get(1)).hasSize(1);
    }
}
