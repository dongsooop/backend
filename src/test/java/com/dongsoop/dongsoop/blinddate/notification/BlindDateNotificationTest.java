package com.dongsoop.dongsoop.blinddate.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import com.dongsoop.dongsoop.role.entity.RoleType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@DisplayName("과팅 개최 알림 경계 결과")
class BlindDateNotificationTest {
    @Test
    @DisplayName("대상 회원이 없으면 저장·전송하지 않음")
    void emptyRecipientsDoNotProduceNotifications() {
        var members = mock(MemberRepository.class);
        var save = mock(NotificationSaveService.class);
        var send = mock(NotificationSendService.class);
        when(members.findByRoleTypeWithDevice(RoleType.ADMIN)).thenReturn(List.of());
        new BlindDateNotificationImpl(save, send, members).send();
        verifyNoInteractions(save, send);
    }

    @Test
    @DisplayName("저장된 개최 알림을 BLINDDATE 유형으로 전달")
    void forwardsPersistedNotifications() {
        var members = mock(MemberRepository.class);
        var save = mock(NotificationSaveService.class);
        var send = mock(NotificationSendService.class);
        var recipients = List.of(mock(Member.class));
        var saved = List.of(mock(MemberNotification.class));
        when(members.findByRoleTypeWithDevice(RoleType.ADMIN)).thenReturn(recipients);
        when(save.saveAll(
                        eq(recipients),
                        anyString(),
                        anyString(),
                        eq(NotificationType.BLINDDATE),
                        eq("")))
                .thenAnswer(
                        call -> {
                            assertThat((String) call.getArgument(1)).isNotBlank();
                            assertThat((String) call.getArgument(2)).isNotBlank();
                            return saved;
                        });
        new BlindDateNotificationImpl(save, send, members).send();
        verify(send).sendAll(saved, NotificationType.BLINDDATE);
    }
}
