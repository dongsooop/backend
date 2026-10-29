package com.dongsoop.dongsoop.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.service.DepartmentService;
import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.member.dto.*;
import com.dongsoop.dongsoop.member.exception.InvalidPasswordFormatException;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberServiceImpl;
import com.dongsoop.dongsoop.member.validate.MemberDuplicationValidator;
import com.dongsoop.dongsoop.memberdevice.entity.*;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.oauth.service.OAuth2Service;
import com.dongsoop.dongsoop.report.validator.ReportValidator;
import com.dongsoop.dongsoop.role.entity.*;
import com.dongsoop.dongsoop.role.repository.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberLoginServiceContractTest {
  @Mock MemberRepository members;
  @Mock RoleRepository roles;
  @Mock MemberRoleRepository memberRoles;
  @Mock DepartmentService departments;
  @Mock PasswordEncoder encoder;
  @Mock TokenGenerator tokens;
  @Mock MemberDuplicationValidator duplicates;
  @Mock ReportValidator reports;
  @Mock MemberDeviceRepository devices;
  @Mock MemberDeviceService deviceService;
  @Mock OAuth2Service oauth;
  @InjectMocks MemberServiceImpl service;
  private static final String EMAIL = "member@dongyang.ac.kr";

  @Test
  void mobileLoginIssuesBothTokensForResolvedDeviceAndMemberAuthorities() {
    prepareCredentials();
    when(devices.findByDeviceToken("mobile"))
        .thenReturn(Optional.of(MemberDevice.builder().id(9L).build()));
    var request = request("mobile", MemberDeviceType.IOS);
    var result = service.login(request);

    assertThat(result.getIssuedToken().getAccessToken()).isEqualTo("access");
    assertThat(result.getIssuedToken().getRefreshToken()).isEqualTo("refresh");
    assertThat(result.getLoginMemberDetail().getId()).isEqualTo(7L);
    var auth = ArgumentCaptor.forClass(Authentication.class);
    verify(tokens).generateAccessToken(auth.capture(), eq(9L));
    verify(tokens).generateRefreshToken(any(Authentication.class), eq(9L));
    assertThat(auth.getValue().getPrincipal()).isEqualTo(7L);
    assertThat(auth.getValue().getCredentials()).isNull();
    assertThat(auth.getValue().getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_USER");
    verify(reports).checkMemberAccessById(7L);
    verifyNoInteractions(deviceService);
  }

  @Test
  void webLoginUsesEffectiveDeviceTokenForJwtDeviceId() {
    prepareCredentials();
    when(deviceService.createAndBindWebDevice(7L, "web-input")).thenReturn("web-effective");
    when(devices.findByDeviceToken("web-effective"))
        .thenReturn(Optional.of(MemberDevice.builder().id(19L).build()));
    service.login(request("web-input", MemberDeviceType.WEB));
    verify(tokens).generateAccessToken(any(Authentication.class), eq(19L));
    verify(tokens).generateRefreshToken(any(Authentication.class), eq(19L));
    verify(devices, never()).findByDeviceToken("web-input");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = "   ")
  void absentTokenPreservesLegacyJwtWithoutDeviceLookup(String token) {
    prepareCredentials();
    service.login(request(token, MemberDeviceType.WEB));
    verify(tokens).generateAccessToken(any(Authentication.class), isNull());
    verify(tokens).generateRefreshToken(any(Authentication.class), isNull());
    verifyNoInteractions(devices, deviceService);
  }

  @Test
  void unknownDevicePreservesLegacyJwt() {
    prepareCredentials();
    when(devices.findByDeviceToken("unknown")).thenReturn(Optional.empty());
    service.login(request("unknown", MemberDeviceType.ANDROID));
    verify(tokens).generateAccessToken(any(Authentication.class), isNull());
    verify(tokens).generateRefreshToken(any(Authentication.class), isNull());
  }

  @Test
  void nonexistentMemberCannotIssueTokens() {
    var request = request("mobile", MemberDeviceType.IOS);
    assertThatThrownBy(() -> service.login(request)).isInstanceOf(MemberNotFoundException.class);
    verifyNoInteractions(tokens, devices, deviceService, reports);
  }

  @Test
  void wrongPasswordCannotResolveDeviceOrIssueTokens() {
    when(members.existsByEmailAndIsDeletedFalse(EMAIL)).thenReturn(true);
    when(members.findLoginAuthenticateByEmail(EMAIL)).thenReturn(Optional.of(credentials()));
    var request = request("mobile", MemberDeviceType.IOS);
    assertThatThrownBy(() -> service.login(request))
        .isInstanceOf(InvalidPasswordFormatException.class);
    verifyNoInteractions(tokens, devices, deviceService, reports);
  }

  @Test
  void tokenIssuingFailureCannotReturnPartialLogin() {
    when(members.existsByEmailAndIsDeletedFalse(EMAIL)).thenReturn(true);
    when(members.findLoginAuthenticateByEmail(EMAIL)).thenReturn(Optional.of(credentials()));
    when(encoder.matches("Valid123!", "encoded")).thenReturn(true);
    when(memberRoles.findAllByMemberId(7L)).thenReturn(List.of());
    when(tokens.generateAccessToken(any(Authentication.class), isNull()))
        .thenThrow(new IllegalStateException("signing unavailable"));
    var request = request(null, MemberDeviceType.WEB);
    assertThatThrownBy(() -> service.login(request)).isInstanceOf(IllegalStateException.class);
    verify(tokens, never()).generateRefreshToken(any(Authentication.class), any());
    verify(members, never()).findLoginMemberDetailById(anyLong());
  }

  private void prepareCredentials() {
    when(members.existsByEmailAndIsDeletedFalse(EMAIL)).thenReturn(true);
    when(members.findLoginAuthenticateByEmail(EMAIL)).thenReturn(Optional.of(credentials()));
    when(encoder.matches("Valid123!", "encoded")).thenReturn(true);
    var role = mock(Role.class);
    when(role.getRoleType()).thenReturn(RoleType.USER);
    when(memberRoles.findAllByMemberId(7L)).thenReturn(List.of(role));
    when(tokens.generateAccessToken(any(Authentication.class), any())).thenReturn("access");
    when(tokens.generateRefreshToken(any(Authentication.class), any())).thenReturn("refresh");
    when(members.findLoginMemberDetailById(7L))
        .thenReturn(
            Optional.of(
                new LoginMemberDetails(
                    7L, "Member", EMAIL, DepartmentType.DEPT_2001, List.of(RoleType.USER))));
  }

  private LoginAuthenticate credentials() {
    return new LoginAuthenticate() {
      public Long getId() {
        return 7L;
      }

      public String getPassword() {
        return "encoded";
      }
    };
  }

  static LoginRequest request(String token, MemberDeviceType type) {
    var request = new LoginRequest();
    ReflectionTestUtils.setField(request, "email", EMAIL);
    ReflectionTestUtils.setField(request, "password", "Valid123!");
    ReflectionTestUtils.setField(request, "fcmToken", token);
    ReflectionTestUtils.setField(request, "deviceType", type);
    return request;
  }
}
