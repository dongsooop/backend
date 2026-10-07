package com.dongsoop.dongsoop.blinddate.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlindDateMatchNotificationTest {

    private final MemberRepository members = mock(MemberRepository.class);
    private final NotificationSaveService storage = mock(NotificationSaveService.class);
    private final NotificationSendService delivery = mock(NotificationSendService.class);
    private final BlindDateMatchNotification notification = new BlindDateMatchNotification(members, storage, delivery);
    private final List<MemberNotification> saved = new ArrayList<>();
    private final List<MemberNotification> sent = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(members.findById(1L)).thenReturn(Optional.of(Member.builder().id(1L).build()));
        when(storage.save(any(Member.class), anyString(), anyString(), eq(NotificationType.CHAT), anyString()))
                .thenAnswer(call -> {
                    NotificationDetails details = NotificationDetails.builder()
                            .title(call.getArgument(1)).body(call.getArgument(2))
                            .type(call.getArgument(3)).value(call.getArgument(4)).build();
                    MemberNotification result = new MemberNotification(details, call.getArgument(0));
                    saved.add(result);
                    return result;
                });
        doAnswer(call -> {
            sent.addAll(call.getArgument(0));
            return null;
        }).when(delivery).sendAll(anyList(), eq(NotificationType.CHAT));
    }

    @Test
    void successIsSavedAndDeliveredWithChatRoomNavigation() {
        notification.send(1L, "room-1");
        assertThat(saved).hasSize(1);
        MemberNotification result = saved.get(0);
        assertThat(result.getId().getMember().getId()).isEqualTo(1L);
        assertThat(result.getId().getDetails().getType()).isEqualTo(NotificationType.CHAT);
        assertThat(result.getId().getDetails().getValue()).isEqualTo("room-1");
        assertThat(result.getId().getDetails().getTitle()).contains("매칭");
        assertThat(result.isRead()).isFalse();
        assertThat(sent).containsExactly(result);
    }

    @Test
    void pushFailureKeepsSavedSuccessNotification() {
        doAnswer(call -> {
            throw new IllegalStateException("push failed");
        }).when(delivery).sendAll(anyList(), eq(NotificationType.CHAT));
        assertThatThrownBy(() -> notification.send(1L, "room-1"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getId().getDetails().getValue()).isEqualTo("room-1");
    }

    @Test
    void storageFailureDoesNotSendAnUnsavedNotification() {
        when(storage.save(any(Member.class), anyString(), anyString(), eq(NotificationType.CHAT), anyString()))
                .thenThrow(new IllegalStateException("storage failed"));
        assertThatThrownBy(() -> notification.send(1L, "room-1"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(saved).isEmpty();
        assertThat(sent).isEmpty();
    }

    @Test
    void removedMemberDoesNotReceiveNotification() {
        when(members.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> notification.send(1L, "room-1"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(saved).isEmpty();
        assertThat(sent).isEmpty();
    }
}
