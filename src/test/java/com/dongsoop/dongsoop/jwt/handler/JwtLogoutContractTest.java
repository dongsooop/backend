package com.dongsoop.dongsoop.jwt.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.exception.DeviceInformationNotIncludedInHeaderException;
import com.dongsoop.dongsoop.jwt.exception.TokenNotFoundException;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notification.service.FCMService;
import io.jsonwebtoken.Jwts;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtLogoutContractTest {
    @Mock MemberDeviceRepository devices;
    @Mock JwtUtil jwt;
    @Mock FCMService fcm;
    @Mock DeviceBlacklistService blacklist;
    @InjectMocks JwtLogoutHandler handler;
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void configureTopic() {
        ReflectionTestUtils.setField(handler, "anonymousTopic", "anonymous-test");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void missingDeviceHeaderCannotInvalidateAnyDevice(String token) {
        var request = request(token, "Bearer access");
        assertThatThrownBy(() -> handler.logout(request, response, null))
                .isInstanceOf(DeviceInformationNotIncludedInHeaderException.class);
        verifyNoInteractions(jwt, devices, blacklist, fcm);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Bearer ", "Basic access", "bearer access", "Bearer"})
    void invalidAuthorizationCannotInvalidateAnyDevice(String authorization) {
        var request = request("device-token", authorization);
        assertThatThrownBy(() -> handler.logout(request, response, null)).isInstanceOf(TokenNotFoundException.class);
        verifyNoInteractions(jwt, devices, blacklist, fcm);
    }

    @Test
    void deviceOwnedByAnotherMemberCannotBeLoggedOut() {
        var request = request("other-device", "Bearer access");
        when(jwt.getClaims("access")).thenReturn(Jwts.claims().subject("7").build());
        when(devices.findByMemberIdAndDeviceToken(7L, "other-device")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.logout(request, response, null)).isInstanceOf(UnregisteredDeviceException.class);
        verifyNoInteractions(blacklist, fcm);
        verify(devices, never()).delete(any(MemberDevice.class));
    }

    @ParameterizedTest
    @EnumSource(value = MemberDeviceType.class, names = {"IOS", "ANDROID"})
    void mobileLogoutUnbindsAndResetsAnonymousDelivery(MemberDeviceType type) {
        var device = registered(type);
        var request = request("device-token", "Bearer access");

        handler.logout(request, response, null);

        assertThat(device.getMember()).isNull();
        assertThat(device.getDeviceToken()).isEqualTo("device-token");
        assertThat(device.getFid()).isEqualTo("installation-id");
        verify(blacklist).blacklist(9L);
        verify(fcm).subscribeTopic(List.of("device-token"), "anonymous-test");
        verify(fcm).updateNotificationBadge(List.of("device-token"), 0);
        verify(devices, never()).delete(any(MemberDevice.class));
    }

    @Test
    void webLogoutDeletesDeviceAfterInvalidatingTokens() {
        var device = registered(MemberDeviceType.WEB);
        handler.logout(request("device-token", "Bearer access"), response, null);

        var order = inOrder(blacklist, devices);
        order.verify(blacklist).blacklist(9L);
        order.verify(devices).delete(device);
        verifyNoInteractions(fcm);
    }

    @Test
    void invalidJwtCannotReachDeviceMutation() {
        var request = request("device-token", "Bearer broken");
        when(jwt.getClaims("broken")).thenThrow(new IllegalArgumentException("invalid token"));
        assertThatThrownBy(() -> handler.logout(request, response, null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(devices, blacklist, fcm);
    }

    private MemberDevice registered(MemberDeviceType type) {
        var device = MemberDevice.builder().id(9L).member(Member.builder().id(7L).build())
                .deviceToken("device-token").fid("installation-id").memberDeviceType(type).build();
        when(jwt.getClaims("access")).thenReturn(Jwts.claims().subject("7").build());
        when(devices.findByMemberIdAndDeviceToken(7L, "device-token")).thenReturn(Optional.of(device));
        return device;
    }

    private MockHttpServletRequest request(String device, String authorization) {
        var request = new MockHttpServletRequest();
        if (device != null) {
            request.addHeader("Device-Token", device);
        }
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }
}
