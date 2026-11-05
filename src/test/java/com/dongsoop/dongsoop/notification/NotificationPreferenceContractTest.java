package com.dongsoop.dongsoop.notification;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.dto.*;
import com.dongsoop.dongsoop.notification.setting.entity.*;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import com.dongsoop.dongsoop.notification.setting.service.NotificationSettingServiceImpl;
import com.dongsoop.dongsoop.notification.setting.service.handler.*;
import com.dongsoop.dongsoop.notification.setting.service.switcher.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceContractTest {
    @Mock MemberService members;
    @Mock MemberDeviceRepository devices;
    @Mock NotificationSettingRepository settings;
    private NotificationSettingServiceImpl service;
    private NotificationSettingSwitcher switcher;
    private final MemberDevice first = MemberDevice.builder().id(9L).deviceToken("a").build();
    private final MemberDevice second = MemberDevice.builder().id(10L).deviceToken("b").build();

    @BeforeEach
    void setup() {
        var operator = new NotificationSettingOperator(List.of(new NotificationSettingChangeCreate(settings),
                new NotificationSettingChangeDelete(settings), new NotificationSettingChangeUpdate(), new NotificationSettingChangeSkip()));
        switcher = new NotificationSettingSwitcher(members, devices, settings, new SettingChangeClassifier(), operator);
        service = new NotificationSettingServiceImpl(settings, switcher);
    }

    @Test
    void storedOverridesWinAndMissingTypesReceiveTheirOwnDefault() {
        when(settings.findByDeviceToken("a")).thenReturn(List.of(new NotificationActiveStatus(NotificationType.CHAT, false),
                new NotificationActiveStatus(NotificationType.MARKETING, true)));
        var result = service.getNotificationSettings(new NotificationSettingFindRequest("a"));
        assertThat(result).hasSize(NotificationType.values().length).containsEntry(NotificationType.CHAT, false)
                .containsEntry(NotificationType.MARKETING, true);
        for (NotificationType type : NotificationType.values()) {
            if (type == NotificationType.CHAT || type == NotificationType.MARKETING) { continue; }
            assertThat(result).containsEntry(type, type.getDefaultActiveState());
        }
    }

    @Test
    void authenticatedChangeCreatesOverrideForEveryOwnedDeviceWithExactTypeAndState() {
        when(members.isAuthenticated()).thenReturn(true);
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        when(devices.findByMemberId(7L)).thenReturn(List.of(first, second));
        service.disableNotification(new NotificationSettingRequest("ignored-guest-token", NotificationType.NOTICE));
        ArgumentCaptor<Iterable<NotificationSetting>> captured = settingCaptor();
        verify(settings).saveAll(captured.capture());
        assertThat(captured.getValue()).extracting(NotificationSetting::getId).containsExactly(
                new NotificationSettingId(9L, NotificationType.NOTICE), new NotificationSettingId(10L, NotificationType.NOTICE));
        assertThat(captured.getValue()).allSatisfy(value -> assertThat(value.getEnabled()).isFalse());
        verify(devices, never()).findByDeviceToken(any());
    }

    @Test
    void guestReturningToDefaultDeletesOverrideAndDoesNotCreateRedundantRow() {
        when(devices.findByDeviceToken("a")).thenReturn(java.util.Optional.of(first));
        NotificationSetting stored = new NotificationSetting(first, NotificationType.NOTICE, false);
        when(settings.findAllById(any())).thenReturn(List.of(stored));
        service.enableNotification(new NotificationSettingRequest("a", NotificationType.NOTICE));
        verify(settings).deleteAll(List.of(stored));
        verify(settings, never()).saveAll(any());
        verify(members, never()).getMemberIdByAuthentication();
    }

    @Test
    void existingStateIsUpdatedOrSkippedWithoutInsertOrDelete() {
        when(members.isAuthenticated()).thenReturn(true);
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        when(devices.findByMemberId(7L)).thenReturn(List.of(first, second));
        NotificationSetting changed = new NotificationSetting(first, NotificationType.NOTICE, true);
        NotificationSetting unchanged = new NotificationSetting(second, NotificationType.NOTICE, false);
        when(settings.findAllById(any())).thenReturn(List.of(changed, unchanged));
        service.disableNotification(new NotificationSettingRequest("a", NotificationType.NOTICE));
        assertThat(changed.getEnabled()).isFalse();
        assertThat(unchanged.getEnabled()).isFalse();
        verify(settings, never()).saveAll(any()); verify(settings, never()).deleteAll(any());
    }

    @Test
    void missingGuestDeviceCannotWriteSettings() {
        NotificationSettingRequest request = new NotificationSettingRequest("missing", NotificationType.NOTICE);
        assertThatThrownBy(() -> service.disableNotification(request)).isInstanceOf(UnregisteredDeviceException.class);
        verifyNoInteractions(settings);
    }

    @Test
    void alreadyDefaultWithoutRowDoesNotInsertOrDelete() {
        when(devices.findByDeviceToken("a")).thenReturn(java.util.Optional.of(first));
        service.enableNotification(new NotificationSettingRequest("a", NotificationType.NOTICE));
        verify(settings, never()).saveAll(any()); verify(settings, never()).deleteAll(any());
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Iterable<NotificationSetting>> settingCaptor() { return ArgumentCaptor.forClass(Iterable.class); }
}
