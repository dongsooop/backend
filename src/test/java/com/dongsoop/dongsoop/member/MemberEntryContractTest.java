package com.dongsoop.dongsoop.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.jwt.dto.IssuedToken;
import com.dongsoop.dongsoop.mailverify.passwordupdate.PasswordUpdateMailValidator;
import com.dongsoop.dongsoop.mailverify.register.RegisterMailValidator;
import com.dongsoop.dongsoop.member.controller.MemberController;
import com.dongsoop.dongsoop.member.dto.*;
import com.dongsoop.dongsoop.member.service.*;
import com.dongsoop.dongsoop.member.validate.MemberDuplicationValidator;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberEntryContractTest {
  @Mock MemberService service;
  @Mock LoginNotificationService loginNotifications;
  @Mock MemberDuplicationValidator duplicates;
  @Mock PasswordUpdateMailValidator passwordMail;
  @Mock RegisterMailValidator registerMail;
  @Mock MemberDeviceService devices;
  @Mock FCMService fcm;
  @InjectMocks MemberController controller;
  private static final String EMAIL = "member@dongyang.ac.kr";

  @BeforeEach
  void configureTopic() {
    ReflectionTestUtils.setField(controller, "anonymousTopic", "anonymous-test");
  }

  @Test
  void mobileLoginBindsDeviceAndNotifiesFirstForwardedAddress() {
    var request = MemberLoginServiceContractTest.request("mobile", MemberDeviceType.IOS);
    prepareLogin(request);
    var http = new MockHttpServletRequest();
    http.addHeader("X-Forwarded-For", " 192.0.2.1 , 192.0.2.2");
    http.addHeader("User-Agent", "Test client");
    var result = controller.login(request, http);

    assertThat(result.getStatusCode().value()).isEqualTo(200);
    assertThat(result.getBody())
        .isNotNull()
        .satisfies(
            body -> {
              assertThat(body.getId()).isEqualTo(7L);
              assertThat(body.getAccessToken()).isEqualTo("access");
              assertThat(body.getRefreshToken()).isEqualTo("refresh");
            });
    verify(devices).bindDeviceWithMemberId(7L, "mobile");
    verify(fcm).unsubscribeTopic(List.of("mobile"), "anonymous-test");
    verify(loginNotifications).sendLoginNotification(7L, EMAIL, "192.0.2.1", "Test client");
  }

  @Test
  void webLoginUsesRemoteAddressWithoutMobileBinding() {
    var request = MemberLoginServiceContractTest.request("web", MemberDeviceType.WEB);
    prepareLogin(request);
    var http = new MockHttpServletRequest();
    http.setRemoteAddr("192.0.2.3");
    http.addHeader("X-Forwarded-For", "   ");
    controller.login(request, http);
    verifyNoInteractions(devices, fcm);
    verify(loginNotifications).sendLoginNotification(7L, EMAIL, "192.0.2.3", null);
  }

  @Test
  void mobileWithoutTokenDoesNotBind() {
    var request = MemberLoginServiceContractTest.request(null, MemberDeviceType.IOS);
    prepareLogin(request);
    controller.login(request, new MockHttpServletRequest());
    verifyNoInteractions(devices, fcm);
  }

  @Test
  void signupAndPasswordUpdateConsumeVerificationOnlyAfterSuccess() {
    var signup = SignupRequest.builder().email(EMAIL).build();
    var password = new UpdatePasswordRequest(EMAIL, "Valid123!");
    assertThat(controller.signup(signup).getStatusCode().value()).isEqualTo(201);
    assertThat(controller.updatePassword(password).getStatusCode().value()).isEqualTo(204);
    var order = inOrder(registerMail, passwordMail, service);
    order.verify(registerMail).validateVerifySuccess(EMAIL);
    order.verify(service).signup(signup);
    order.verify(registerMail).removeVerificationCode(EMAIL);
    order.verify(passwordMail).validateVerifySuccess(EMAIL);
    order.verify(service).updatePassword(password);
    order.verify(passwordMail).removeVerificationCode(EMAIL);
  }

  @Test
  void failedSignupPreservesVerificationCode() {
    var request = SignupRequest.builder().email(EMAIL).build();
    doThrow(new IllegalStateException("storage failed")).when(service).signup(request);
    assertThatThrownBy(() -> controller.signup(request)).isInstanceOf(IllegalStateException.class);
    verify(registerMail, never()).removeVerificationCode(anyString());
  }

  @Test
  void failedPasswordVerificationCannotUpdateOrConsumeCode() {
    var request = new UpdatePasswordRequest(EMAIL, "Valid123!");
    doThrow(new IllegalArgumentException("verification required"))
        .when(passwordMail)
        .validateVerifySuccess(EMAIL);
    assertThatThrownBy(() -> controller.updatePassword(request))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(service);
    verify(passwordMail, never()).removeVerificationCode(anyString());
  }

  @Test
  void duplicationValidationAndDeletionReturnNoContent() {
    assertThat(controller.validateEmail(new EmailValidateRequest(EMAIL)).getStatusCode().value())
        .isEqualTo(204);
    assertThat(
            controller
                .validateNickname(new NicknameValidateRequest("Member"))
                .getStatusCode()
                .value())
        .isEqualTo(204);
    assertThat(controller.deleteMember().getStatusCode().value()).isEqualTo(204);
    verify(duplicates).validateEmailDuplication(EMAIL);
    verify(duplicates).validateNicknameDuplication("Member");
    verify(service).deleteMember();
  }

  private void prepareLogin(LoginRequest request) {
    when(service.login(request))
        .thenReturn(
            new LoginDetails(
                new LoginMemberDetails(
                    7L, "Member", EMAIL, DepartmentType.DEPT_2001, List.of(RoleType.USER)),
                new IssuedToken("access", "refresh")));
  }
}
