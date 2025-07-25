package com.dongsoop.dongsoop.mailverify.service;

import com.dongsoop.dongsoop.mailverify.exception.VerifyMailCodeNotAvailableException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailVerifyServiceImpl implements MailVerifyService {

    private static final String SUBJECT = "[동숲] 인증번호 발송";
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final int VERIFY_CODE_EXPIRATION_TIME = 300; // 5minute
    private static final String VERIFY_CODE_KEY_PREFIX = "mail-verify:";

    private final JavaMailSender sender;
    private final MailTextGenerator mailTextGenerator;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${mail.sender}")
    private String senderEmail;

    @Override
    public void sendMail(String to) throws MessagingException {
        String verifyCode = generateVerificationCode();

        String redisKey = VERIFY_CODE_KEY_PREFIX + to;
        redisTemplate.opsForValue()
                .set(redisKey, verifyCode);
        redisTemplate.expire(redisKey, Duration.ofSeconds(VERIFY_CODE_EXPIRATION_TIME));

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(senderEmail);   // 내 도메인 주소
        helper.setTo(to);
        helper.setSubject(SUBJECT);
        helper.setText(mailTextGenerator.generateVerificationText(verifyCode), true);
        sender.send(message);
    }

    private String generateVerificationCode() {
        StringBuilder codeBuilder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = (int) (Math.random() * CHAR_POOL.length());
            codeBuilder.append(CHAR_POOL.charAt(index));
        }
        return codeBuilder.toString();
    }

    @Override
    public void validateVerificationCode(String email, String code) {
        String redisKey = VERIFY_CODE_KEY_PREFIX + email;
        String storedCode = redisTemplate.opsForValue()
                .get(redisKey);
        redisTemplate.delete(redisKey);

        if (storedCode == null || !storedCode.equals(code)) {
            throw new VerifyMailCodeNotAvailableException();
        }
    }
}
