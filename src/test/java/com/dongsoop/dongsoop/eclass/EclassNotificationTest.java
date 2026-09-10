package com.dongsoop.dongsoop.eclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.eclass.notification.EclassNotificationImpl;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EclassNotificationTest {

    private static final LocalDateTime DUE_AT = LocalDateTime.of(2026, 9, 25, 23, 55);

    @Mock
    private NotificationSendService notificationSendService;
    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @InjectMocks
    private EclassNotificationImpl eclassNotification;

    private EclassLink link;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eclassNotification, "baseUrl", "https://eclass.dongyang.ac.kr");
        ReflectionTestUtils.setField(eclassNotification, "courseNameMaxLength", 20);
        MemberDevice device = MemberDevice.builder()
                .id(1L)
                .deviceToken("fcm-token")
                .build();
        link = new EclassLink(device, "백승민", "encrypted");
    }

    private NotificationSend sentMessage() {
        ArgumentCaptor<NotificationSend> captor = ArgumentCaptor.forClass(NotificationSend.class);
        verify(notificationSendService).send(eq(List.of("fcm-token")), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("리마인드 제목은 과목명과 남은 날짜로, 본문은 과제명과 마감 시각으로, 링크는 코스모듈 번호로 만든다")
    void reminderMessage() {
        eclassNotification.sendReminder(new EclassAssignment(link, 1L, 9101L, "자료구조", "3주차_과제", DUE_AT, DUE_AT), 3);

        NotificationSend sent = sentMessage();
        assertThat(sent.title()).isEqualTo("[자료구조] 과제 3일 전입니다");
        assertThat(sent.body()).isEqualTo("3주차_과제 · 마감 9월 25일 (금) 23:55");
        assertThat(sent.value()).isEqualTo("https://eclass.dongyang.ac.kr/mod/assign/view.php?id=9101");
    }

    @Test
    @DisplayName("당일은 '오늘 마감'으로 쓴다")
    void todayTitle() {
        eclassNotification.sendReminder(new EclassAssignment(link, 1L, 9101L, "자료구조", "과제", DUE_AT, DUE_AT), 0);

        assertThat(sentMessage().title()).isEqualTo("[자료구조] 과제 오늘 마감입니다");
    }

    @Test
    @DisplayName("과목명이 길면 자르고 말줄임표를 붙인다")
    void shortensLongCourseName() {
        String courseName = "가나다라마바사아자차카타파하가나다라마바사";
        eclassNotification.sendReminder(new EclassAssignment(link, 1L, 9101L, courseName, "과제", DUE_AT, DUE_AT), 3);

        assertThat(sentMessage().title()).isEqualTo("[가나다라마바사아자차카타파하가나다라마바…] 과제 3일 전입니다");
    }

    @Test
    @DisplayName("마감이 앞당겨지면 그 사실을 제목에 쓴다")
    void dueDateChangedTitle() {
        eclassNotification.sendDueDateChanged(link, new EclassAssignment(link, 1L, 9101L, "자료구조", "과제", DUE_AT, DUE_AT));

        assertThat(sentMessage().title()).isEqualTo("[자료구조] 과제 마감이 앞당겨졌어요");
    }

    @Test
    @DisplayName("만료 안내는 과제 알림 설정을 보지 않고 보낸다")
    void expiredNoticeIgnoresSetting() {
        eclassNotification.sendExpiredNotice(link);

        verify(notificationSettingRepository, never()).findByIdDeviceIdAndIdNotificationType(any(), any());
        assertThat(sentMessage().title()).isEqualTo("이클래스 연동이 만료되었습니다");
    }
}
