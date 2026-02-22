package com.dongsoop.dongsoop.member.service;

import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 로그인 알림 서비스 구현체.
 *
 * <p>로그인 감지 시 이메일 알림과 FCM 푸시 알림을 발송한다.
 * 이메일 발송 실패 시 예외를 전파하지 않고 로그만 기록하여 로그인 흐름에 영향을 주지 않는다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LoginNotificationServiceImpl implements LoginNotificationService {

    private static final String LOGIN_MAIL_SUBJECT = "[동숲] 계정 로그인 알림";
    private static final String PUSH_TITLE = "로그인 감지";
    private static final String PUSH_BODY = "계정에 새로운 로그인이 감지되었습니다. 본인이 아니라면 비밀번호를 변경해 주세요.";

    private final MemberDeviceService memberDeviceService;
    private final NotificationSendService notificationSendService;
    private final JavaMailSender mailSender;

    @Value("${mail.sender}")
    private String senderEmail;

    @Value("${resource.static.base-path}")
    private String staticResourceBasePath;

    @Value("${mail.login-notification.template}")
    private String loginMailTemplate;

    /**
     * {@inheritDoc}
     *
     * <p>이메일 발송과 FCM 푸시 알림을 순차적으로 실행한다.
     * 이메일 발송 실패는 로그로 기록되며 푸시 알림 발송은 계속 진행된다.
     */
    @Override
    public void sendLoginNotification(Long memberId, String email) {
        sendEmail(email);
        sendPushNotification(memberId);
    }

    /**
     * 로그인 알림 이메일을 발송한다.
     *
     * <p>HTML 템플릿을 로드하여 지정된 이메일 주소로 발송한다.
     * 발송 실패 시 예외를 전파하지 않고 에러 로그만 기록한다.
     *
     * @param email 수신자 이메일 주소
     */
    private void sendEmail(String email) {
        try {
            String htmlContent = loadHtmlTemplate();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(email);
            helper.setSubject(LOGIN_MAIL_SUBJECT);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            log.error("Failed to send login notification email to {}", maskEmail(email), e);
        }
    }

    /**
     * 클래스패스에서 HTML 이메일 템플릿을 로드한다.
     *
     * @return UTF-8로 인코딩된 HTML 문자열
     * @throws IOException 템플릿 파일을 읽을 수 없는 경우
     */
    private String loadHtmlTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource(staticResourceBasePath + loginMailTemplate);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String masked = local.length() <= 2 ? "****" : local.substring(0, 2) + "****";
        return masked + "@" + parts[1];
    }

    /**
     * 회원의 모든 등록 기기에 FCM 푸시 알림을 발송한다.
     *
     * <p>등록된 기기가 없는 경우 발송하지 않고 종료한다.
     *
     * @param memberId 푸시 알림을 수신할 회원의 ID
     */
    private void sendPushNotification(Long memberId) {
        List<String> deviceTokens = memberDeviceService.getDeviceByMemberId(memberId);
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            return;
        }
        NotificationSend push = new NotificationSend(0L, PUSH_TITLE, PUSH_BODY, NotificationType.NEW_DEVICE_LOGIN, "");
        notificationSendService.send(deviceTokens, push);
    }
}
