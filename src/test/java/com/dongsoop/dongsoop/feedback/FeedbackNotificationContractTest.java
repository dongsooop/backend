package com.dongsoop.dongsoop.feedback;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.feedback.notification.FeedbackNotificationImpl;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedbackNotificationContractTest {
    @Mock MemberRepository members;
    @Mock NotificationSaveService storage;
    @Mock NotificationSendService sender;
    @InjectMocks FeedbackNotificationImpl notification;
    private static final String BODY = "새로운 피드백이 등록되었습니다! 내용을 확인해주세요!";

    @Test
    void feedbackNotificationUsesAdministratorsAndSavedNotificationPayload() {
        var admins = List.of(Member.builder().id(7L).build());
        var saved = List.of(mock(MemberNotification.class));
        when(members.findByRoleTypeWithDevice(RoleType.ADMIN)).thenReturn(admins);
        when(storage.saveAll(admins, "Improve search", BODY, NotificationType.FEEDBACK, "19")).thenReturn(saved);
        notification.send("Improve search", 19L);
        var order = inOrder(storage, sender);
        order.verify(storage).saveAll(admins, "Improve search", BODY, NotificationType.FEEDBACK, "19");
        order.verify(sender).sendAll(saved, NotificationType.FEEDBACK);
    }

    @Test
    void noAdministratorsMeansNoStorageOrDelivery() {
        when(members.findByRoleTypeWithDevice(RoleType.ADMIN)).thenReturn(List.of());
        notification.send("Improve search", 19L);
        verifyNoInteractions(storage, sender);
    }

    @Test
    void failedNotificationStorageCannotDeliver() {
        var admins = List.of(Member.builder().id(7L).build());
        when(members.findByRoleTypeWithDevice(RoleType.ADMIN)).thenReturn(admins);
        when(storage.saveAll(admins, "Improve search", BODY, NotificationType.FEEDBACK, "19"))
                .thenThrow(new IllegalStateException("notification storage unavailable"));
        assertThatThrownBy(() -> notification.send("Improve search", 19L)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(sender);
    }
}
