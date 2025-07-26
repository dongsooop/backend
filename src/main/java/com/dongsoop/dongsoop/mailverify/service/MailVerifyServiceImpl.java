package com.dongsoop.dongsoop.mailverify.service;

import com.dongsoop.dongsoop.mailverify.exception.UsingAllMailVerifyOpportunityException;
import com.dongsoop.dongsoop.mailverify.exception.VerifyMailCodeNotAvailableException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailVerifyServiceImpl implements MailVerifyService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String SUBJECT = "[동숲] 인증번호 발송";
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final int VERIFY_CODE_EXPIRATION_TIME = 300; // 5minute
    private static final String VERIFY_CODE_KEY_PREFIX = "mail-verify:";
    private static final String VERIFY_CODE_KEY = "code";
    private static final String OPPORTUNITY_KEY = "opportunity";

    private final JavaMailSender sender;
    private final MailTextGenerator mailTextGenerator;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${mail.sender}")
    private String senderEmail;

    @Override
    public void sendMail(String to) throws MessagingException {
        String verifyCode = generateVerificationCode();

        String redisKey = VERIFY_CODE_KEY_PREFIX + to;
        redisTemplate.opsForHash()
                .putAll(redisKey, Map.of(VERIFY_CODE_KEY, verifyCode, OPPORTUNITY_KEY, 3));
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
            int index = secureRandom.nextInt(CHAR_POOL.length());
            codeBuilder.append(CHAR_POOL.charAt(index));
        }
        return codeBuilder.toString();
    }

    @Override
    public void validateVerificationCode(String email, String code) {
        String redisKey = VERIFY_CODE_KEY_PREFIX + email;

        Integer storedOpportunity = getStoredOpportunity(redisKey);
        if (storedOpportunity == null || storedOpportunity <= 0) {
            throw new UsingAllMailVerifyOpportunityException();
        }

        String storedCode = getStoredVerifyCode(redisKey);
        if (storedCode == null || !storedCode.equals(code)) {
            redisTemplate.opsForHash()
                    .increment(redisKey, OPPORTUNITY_KEY, -1);
            throw new VerifyMailCodeNotAvailableException();
        }

        redisTemplate.delete(redisKey);
    }

    private Integer getStoredOpportunity(String redisKey) {
        Object storedOpportunityObject = redisTemplate.opsForHash()
                .get(redisKey, OPPORTUNITY_KEY);

        if (storedOpportunityObject instanceof Integer) {
            return (Integer) storedOpportunityObject;
        }

        return null;
    }

    private String getStoredVerifyCode(String redisKey) {
        Object storedVerifyCodeObject = redisTemplate.opsForHash()
                .get(redisKey, VERIFY_CODE_KEY);

        if (storedVerifyCodeObject instanceof String) {
            return String.valueOf(storedVerifyCodeObject);
        }

        return null;
    }
}
