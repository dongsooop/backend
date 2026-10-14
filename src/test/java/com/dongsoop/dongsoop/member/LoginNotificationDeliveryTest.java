package com.dongsoop.dongsoop.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.LoginNotificationServiceImpl;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
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
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LoginNotificationDeliveryTest {
    @Mock MemberRepository members;
    @Mock NotificationSaveService save;
    @Mock NotificationSendService send;
    @Mock JavaMailSender mail;
    @InjectMocks LoginNotificationServiceImpl service;
    private final Member member = Member.builder().id(7L).build();
    private final MemberNotification notification = new MemberNotification(null, member);

    @BeforeEach
    void configureResources() {
        ReflectionTestUtils.setField(service, "senderEmail", "sender@example.com");
        ReflectionTestUtils.setField(service, "staticResourceBasePath", "static/");
        ReflectionTestUtils.setField(service, "loginMailTemplate", "login-notification-mail.html");
        when(members.getReferenceById(7L)).thenReturn(member);
    }

    @Test
    void sendsMailWithEscapedContextAndSavedPush() throws Exception {
        MimeMessage message = prepareMail();
        preparePush();
        service.sendLoginNotification(7L, "member@example.com", "<script>ip</script>", "Browser & Device");
        verify(mail).send(message);
        assertThat(message.getSubject()).isEqualTo("[동숲] 계정 로그인 알림");
        assertThat(message.getAllRecipients()[0]).hasToString("member@example.com");
        assertThat(message.getFrom()[0]).hasToString("sender@example.com");
        String body = content(message.getContent());
        assertThat(body).contains("&lt;script&gt;ip&lt;/script&gt;", "Browser &amp; Device")
                .doesNotContain("{{loginTime}}", "{{ipAddress}}", "{{deviceInfo}}", "<script>ip</script>");
        verify(send).send(notification);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void absentContextUsesFallbackWithoutFailingLogin(String context) throws Exception {
        MimeMessage message = prepareMail();
        preparePush();
        service.sendLoginNotification(7L, "member@example.com", context, context);
        assertThat(content(message.getContent())).contains("알 수 없음");
        verify(send).send(notification);
    }

    @ParameterizedTest
    @EnumSource(DeliveryFailure.class)
    void deliveryFailureDoesNotFailLogin(DeliveryFailure failure) {
        MimeMessage message = configureFailure(failure);
        assertThatCode(() -> service.sendLoginNotification(7L, "member@example.com", "127.0.0.1", "Browser"))
                .doesNotThrowAnyException();
        if (failure == DeliveryFailure.TEMPLATE) {
            verify(mail, never()).send(any(MimeMessage.class));
            verify(send).send(notification);
            return;
        }
        verify(mail).send(message);
        if (failure == DeliveryFailure.STORAGE) {
            verify(send, never()).send(any(MemberNotification.class));
            return;
        }
        verify(send).send(notification);
    }

    private MimeMessage configureFailure(DeliveryFailure failure) {
        if (failure == DeliveryFailure.TEMPLATE) {
            ReflectionTestUtils.setField(service, "loginMailTemplate", "missing-test-template.html");
            preparePush();
            return null;
        }
        MimeMessage message = prepareMail();
        if (failure == DeliveryFailure.STORAGE) {
            when(save.save(eq(member), any(), any(), eq(NotificationType.NEW_DEVICE_LOGIN), eq("")))
                    .thenThrow(new IllegalStateException("storage unavailable"));
            return message;
        }
        preparePush();
        if (failure == DeliveryFailure.MAIL) {
            doThrow(new MailSendException("unavailable")).when(mail).send(message);
        }
        if (failure == DeliveryFailure.PUSH) {
            doThrow(new IllegalStateException("push unavailable")).when(send).send(notification);
        }
        return message;
    }

    private enum DeliveryFailure {
        MAIL, TEMPLATE, STORAGE, PUSH
    }

    private MimeMessage prepareMail() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mail.createMimeMessage()).thenReturn(message);
        return message;
    }

    private void preparePush() {
        when(save.save(member, "로그인 감지", "계정에 새로운 로그인이 감지되었습니다. 본인이 아니라면 비밀번호를 변경해 주세요.",
                NotificationType.NEW_DEVICE_LOGIN, "")).thenReturn(notification);
    }

    private String content(Object value) throws Exception {
        if (value instanceof String text) {
            return text;
        }
        if (value instanceof Multipart multipart) {
            StringBuilder body = new StringBuilder();
            for (int index = 0; index < multipart.getCount(); index++) {
                body.append(content(multipart.getBodyPart(index).getContent()));
            }
            return body.toString();
        }
        throw new IllegalStateException("Unexpected mail content");
    }
}
