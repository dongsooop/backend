package com.dongsoop.dongsoop.member.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter LOGIN_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MemberRepository memberRepository;
    private final NotificationSaveService notificationSaveService;
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
    public void sendLoginNotification(Long memberId, String email, String ipAddress, String userAgent) {
        sendEmail(email, ipAddress, userAgent);
        sendPushNotification(memberId);
    }

    /**
     * 로그인 알림 이메일을 발송한다.
     *
     * <p>HTML 템플릿을 로드하고 로그인 컨텍스트(시간·IP·기기)를 주입하여 지정된 이메일 주소로 발송한다.
     * 발송 실패 시 예외를 전파하지 않고 에러 로그만 기록한다.
     *
     * @param email     수신자 이메일 주소
     * @param ipAddress 로그인 클라이언트 IP
     * @param userAgent 로그인 클라이언트 User-Agent
     */
    private void sendEmail(String email, String ipAddress, String userAgent) {
        try {
            String htmlContent = loadHtmlTemplate();
            htmlContent = injectLoginContext(htmlContent, ipAddress, userAgent);
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

    /**
     * HTML 템플릿의 플레이스홀더에 로그인 컨텍스트를 주입한다.
     *
     * @param html      원본 HTML 문자열
     * @param ipAddress 로그인 클라이언트 IP (null이면 "알 수 없음")
     * @param userAgent User-Agent 헤더 (null이면 "알 수 없음")
     * @return 플레이스홀더가 치환된 HTML 문자열
     */
    private String injectLoginContext(String html, String ipAddress, String userAgent) {
        String loginTime = LocalDateTime.now().format(LOGIN_TIME_FORMATTER);
        String ip = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : "알 수 없음";
        String device = (userAgent != null && !userAgent.isBlank()) ? userAgent : "알 수 없음";
        return html
                .replace("{{loginTime}}", loginTime)
                .replace("{{ipAddress}}", ip)
                .replace("{{deviceInfo}}", device);
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
     * <p>알림을 {@link NotificationSaveService}로 저장한 뒤 반환된 {@link MemberNotification}을
     * {@link NotificationSendService}로 전달하여 발송한다. 디바이스 조회 및 뱃지 카운트 처리는
     * {@link NotificationSendService} 내부에서 수행된다.
     *
     * @param memberId 푸시 알림을 수신할 회원의 ID
     */
    private void sendPushNotification(Long memberId) {
        Member member = memberRepository.getReferenceById(memberId);
        MemberNotification notification = notificationSaveService.save(
                member, PUSH_TITLE, PUSH_BODY, NotificationType.NEW_DEVICE_LOGIN, "");
        notificationSendService.send(notification);
    }
}
