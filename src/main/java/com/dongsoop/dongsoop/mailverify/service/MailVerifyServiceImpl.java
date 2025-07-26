package com.dongsoop.dongsoop.mailverify.service;

import com.dongsoop.dongsoop.mailverify.exception.UnknownEmailEncodeException;
import com.dongsoop.dongsoop.mailverify.exception.UnknownMailMessagingException;
import com.dongsoop.dongsoop.mailverify.exception.UsingAllMailVerifyOpportunityException;
import com.dongsoop.dongsoop.mailverify.exception.VerifyMailCodeNotAvailableException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    private static final String SUBJECT = "[동숲] 인증번호 발송";
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final int DEFAULT_OPPORTUNITY_COUNT = 3;
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
    public void sendMail(String to) {
        String verifyCode = generateVerificationCode();

        String redisKey = getRedisKeyByHashed(to);
        redisTemplate.opsForHash()
                .putAll(redisKey, Map.of(VERIFY_CODE_KEY, verifyCode, OPPORTUNITY_KEY, DEFAULT_OPPORTUNITY_COUNT));
        redisTemplate.expire(redisKey, Duration.ofSeconds(VERIFY_CODE_EXPIRATION_TIME));

        try {
            sendMessage(to, verifyCode);
        } catch (MessagingException exception) {
            throw new UnknownMailMessagingException(exception);
        }
    }

    private void sendMessage(String to, String verifyCode) throws MessagingException {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(senderEmail); // 발송자 이메일
        helper.setTo(to);
        helper.setSubject(SUBJECT);
        helper.setText(mailTextGenerator.generateVerificationText(verifyCode), true);
        sender.send(message);
    }

    private String generateVerificationCode() {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder codeBuilder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = secureRandom.nextInt(CHAR_POOL.length());
            codeBuilder.append(CHAR_POOL.charAt(index));
        }
        return codeBuilder.toString();
    }

    @Override
    public void validateVerificationCode(String email, String code) {
        String redisKey = getRedisKeyByHashed(email);

        Integer storedOpportunity = getTypedValueFromRedisHash(redisKey, VERIFY_CODE_KEY, Integer.class);
        if (storedOpportunity == null || storedOpportunity <= 0) {
            throw new UsingAllMailVerifyOpportunityException();
        }

        String storedCode = getTypedValueFromRedisHash(redisKey, VERIFY_CODE_KEY, String.class);
        if (storedCode == null || !storedCode.equals(code)) {
            redisTemplate.opsForHash()
                    .increment(redisKey, OPPORTUNITY_KEY, -1);
            throw new VerifyMailCodeNotAvailableException();
        }

        redisTemplate.delete(redisKey);
    }

    private String getRedisKeyByHashed(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(email.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return VERIFY_CODE_KEY_PREFIX + hexString;
        } catch (NoSuchAlgorithmException exception) {
            throw new UnknownEmailEncodeException(exception);
        }
    }

    private <T> T getTypedValueFromRedisHash(String redisKey, String hashKey, Class<T> type) {
        Object value = redisTemplate.opsForHash()
                .get(redisKey, hashKey);

        if (type.isInstance(value)) {
            return type.cast(value);
        }

        return null;
    }
}
