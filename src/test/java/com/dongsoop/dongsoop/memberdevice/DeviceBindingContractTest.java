package com.dongsoop.dongsoop.memberdevice;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.dto.*;
import com.dongsoop.dongsoop.memberdevice.entity.*;
import com.dongsoop.dongsoop.memberdevice.exception.*;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceServiceImpl;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceBindingContractTest {
  @Mock MemberDeviceRepository devices;
  @Mock MemberRepository members;
  @InjectMocks MemberDeviceServiceImpl service;
  private final Member member = Member.builder().id(7L).build();

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "new-fid"})
  void knownDeviceRotatesTokenAndOnlyUpdatesPresentFid(String fid) {
    MemberDevice device = device(MemberDeviceType.IOS);
    when(devices.findById(9L)).thenReturn(Optional.of(device));
    service.registerDevice("rotated", fid, MemberDeviceType.IOS, 9L);
    assertThat(device.getDeviceToken()).isEqualTo("rotated");
    String expected = "original-fid";
    if ("new-fid".equals(fid)) {
      expected = fid;
    }
    assertThat(device.getFid()).isEqualTo(expected);
    verify(devices, never()).save(any());
  }

  @Test
  void missingOrDuplicateKnownDeviceCannotRotateToken() {
    assertThatThrownBy(() -> service.registerDevice("new", "fid", MemberDeviceType.IOS, 9L))
        .isInstanceOf(UnregisteredDeviceException.class);
    MemberDevice device = device(MemberDeviceType.IOS);
    when(devices.findById(9L)).thenReturn(Optional.of(device));
    when(devices.existsByDeviceToken("new")).thenReturn(true);
    assertThatThrownBy(() -> service.registerDevice("new", "fid", MemberDeviceType.IOS, 9L))
        .isInstanceOf(AlreadyRegisteredDeviceException.class);
    assertThat(device.getDeviceToken()).isEqualTo("original");
  }

  @Test
  void memberOwnedFidCannotBeRegisteredAsGuest() {
    when(devices.findByFid("original-fid")).thenReturn(Optional.of(device(MemberDeviceType.IOS)));
    assertThatThrownBy(
            () -> service.registerDevice("rotated", "original-fid", MemberDeviceType.IOS, null))
        .isInstanceOf(AlreadyRegisteredDeviceException.class);
    verify(devices, never()).save(any());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "provided-token"})
  void webBindingReturnsEffectiveTokenAndStoresOwnedWebDevice(String token) {
    when(members.findById(7L)).thenReturn(Optional.of(member));
    String effective = service.createAndBindWebDevice(7L, token);
    ArgumentCaptor<MemberDevice> saved = ArgumentCaptor.forClass(MemberDevice.class);
    verify(devices).save(saved.capture());
    assertThat(saved.getValue().getDeviceToken()).isEqualTo(effective);
    assertThat(saved.getValue().getMember()).isSameAs(member);
    assertThat(saved.getValue().getMemberDeviceType()).isEqualTo(MemberDeviceType.WEB);
    if ("provided-token".equals(token)) {
      assertThat(effective).isEqualTo(token);
      return;
    }
    assertThatCode(() -> UUID.fromString(effective)).doesNotThrowAnyException();
  }

  @Test
  void webBindingRejectsMissingMemberAndDuplicateToken() {
    assertThatThrownBy(() -> service.createAndBindWebDevice(7L, "token"))
        .isInstanceOf(MemberNotFoundException.class);
    when(members.findById(7L)).thenReturn(Optional.of(member));
    when(devices.existsByDeviceToken("token")).thenReturn(true);
    assertThatThrownBy(() -> service.createAndBindWebDevice(7L, "token"))
        .isInstanceOf(AlreadyRegisteredDeviceException.class);
    verify(devices, never()).save(any());
  }

  @Test
  void mobileBindingRequiresDeviceAndMemberThenPersistsRelationship() {
    assertThatThrownBy(() -> service.bindDeviceWithMemberId(7L, "token"))
        .isInstanceOf(UnregisteredDeviceException.class);
    MemberDevice device = MemberDevice.builder().deviceToken("token").build();
    when(devices.findByDeviceToken("token")).thenReturn(Optional.of(device));
    assertThatThrownBy(() -> service.bindDeviceWithMemberId(7L, "token"))
        .isInstanceOf(MemberNotFoundException.class);
    when(members.findById(7L)).thenReturn(Optional.of(member));
    service.bindDeviceWithMemberId(7L, "token");
    assertThat(device.getMember()).isSameAs(member);
    verify(devices).save(device);
  }

  @Test
  void notificationTargetsAreGroupedByOwnerWithoutMixingTokens() {
    var condition = new MemberDeviceFindCondition(List.of(7L, 8L), NotificationType.NOTICE);
    when(devices.findDevicesWithNotification(condition))
        .thenReturn(
            List.of(
                new MemberDeviceDto(member, "a"),
                new MemberDeviceDto(Member.builder().id(8L).build(), "b"),
                new MemberDeviceDto(member, "c")));
    var result = service.getDeviceByMember(condition);
    assertThat(result).containsOnlyKeys(7L, 8L);
    assertThat(result.get(7L)).containsExactly("a", "c");
    assertThat(result.get(8L)).containsExactly("b");
  }

  @Test
  void ownedTokenLookupRejectsMissingUnboundAndForeignDevices() {
    assertThatThrownBy(() -> service.getDeviceTokenIfOwned(7L, 9L))
        .isInstanceOf(UnregisteredDeviceException.class);
    MemberDevice device = device(MemberDeviceType.IOS);
    when(devices.findById(9L)).thenReturn(Optional.of(device));
    assertThat(service.getDeviceTokenIfOwned(7L, 9L)).isEqualTo("original");
    assertThatThrownBy(() -> service.getDeviceTokenIfOwned(8L, 9L))
        .isInstanceOf(UnauthorizedDeviceAccessException.class);
    device.bindMember(null);
    assertThatThrownBy(() -> service.getDeviceTokenIfOwned(7L, 9L))
        .isInstanceOf(UnauthorizedDeviceAccessException.class);
  }

  @ParameterizedTest
  @EnumSource(MemberDeviceType.class)
  void logoutDeletesWebRowButRetainsMobileRowAndToken(MemberDeviceType type) {
    MemberDevice device = device(type);
    when(devices.findById(9L)).thenReturn(Optional.of(device));
    service.unbindDevice(9L);
    if (type == MemberDeviceType.WEB) {
      verify(devices).delete(device);
      return;
    }
    assertThat(device.getMember()).isNull();
    assertThat(device.getDeviceToken()).isEqualTo("original");
    verify(devices, never()).delete(any());
  }

  @Test
  void expiredTokenInvalidationKeepsMemberAndFidAndMissingDeviceIsNoop() {
    MemberDevice device = device(MemberDeviceType.IOS);
    when(devices.findByDeviceToken("original")).thenReturn(Optional.of(device));
    service.unbindByToken("original");
    service.unbindByToken("missing");
    service.unbindDevice(-1L);
    assertThat(device.getDeviceToken()).isNull();
    assertThat(device.getMember()).isSameAs(member);
    assertThat(device.getFid()).isEqualTo("original-fid");
    verify(devices, never()).delete(any());
  }

  @Test
  void accessTimestampMovesIntoInvocationWindow() {
    MemberDevice device = device(MemberDeviceType.IOS);
    when(devices.findById(9L)).thenReturn(Optional.of(device));
    LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    service.updateLastAccessAsync(9L);
    LocalDateTime after = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    assertThat(device)
        .extracting("lastAccess")
        .satisfies(value -> assertThat((LocalDateTime) value).isBetween(before, after));
    service.updateLastAccessAsync(-1L);
  }

  private MemberDevice device(MemberDeviceType type) {
    return MemberDevice.builder()
        .id(9L)
        .member(member)
        .deviceToken("original")
        .fid("original-fid")
        .memberDeviceType(type)
        .build();
  }
}
