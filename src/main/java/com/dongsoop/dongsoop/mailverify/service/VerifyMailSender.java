package com.dongsoop.dongsoop.mailverify.service;

import com.dongsoop.dongsoop.mailverify.exception.UnknownMailMessagingException;
import com.dongsoop.dongsoop.mailverify.mailgenerator.MailTextGenerator;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

public abstract class VerifyMailSender {

    protected static final Integer DEFAULT_CODE_LENGTH = 6;
    protected static final int DEFAULT_OPPORTUNITY_COUNT = 3;

    protected final String opportunityKey;
    protected final String codeKey;
    protected final String successKey;
    protected final RedisTemplate<String, Object> redisTemplate;
    protected final JavaMailSender mailSender;
    protected final MailVerifyNumberGenerator mailVerifyNumberGenerator;
    protected final MailTextGenerator mailTextGenerator;

    @Value("${mail.sender}")
    protected String senderEmail;

    protected VerifyMailSender(RedisTemplate<String, Object> redisTemplate,
                               JavaMailSender mailSender,
                               MailVerifyNumberGenerator mailVerifyNumberGenerator,
                               MailTextGenerator mailTextGenerator,
                               String opportunityKey,
                               String codeKey,
                               String successKey) {
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
        this.mailVerifyNumberGenerator = mailVerifyNumberGenerator;
        this.mailTextGenerator = mailTextGenerator;
        this.opportunityKey = opportunityKey;
        this.codeKey = codeKey;
        this.successKey = successKey;
    }

    protected abstract String getSubject();

    protected abstract int getOpportunityCount();

    protected abstract int getCodeLength();

    protected abstract String getVerifyCodeKeyPrefix();

    protected abstract Integer getVerifyCodeExpirationTime();

    public void send(String userEmail) {
        String verifyCode = mailVerifyNumberGenerator.generateVerificationCode(getCodeLength());
        String redisKey = mailVerifyNumberGenerator.getRedisKeyByHashed(getVerifyCodeKeyPrefix(), userEmail);

        redisTemplate.opsForHash()
                .putAll(redisKey,
                        Map.of(codeKey, verifyCode,
                                opportunityKey, getOpportunityCount(),
                                successKey, false));
        redisTemplate.expire(redisKey, Duration.ofSeconds(getVerifyCodeExpirationTime()));

        try {
            sendMessage(userEmail, verifyCode);
        } catch (MessagingException exception) {
            throw new UnknownMailMessagingException(exception);
        }
    }

    protected void sendMessage(String userEmail, String verifyCode) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(senderEmail); // 발송자 이메일
        helper.setTo(userEmail);
        helper.setSubject(getSubject());
        helper.setText(mailTextGenerator.generateVerificationText(verifyCode), true);
        mailSender.send(message);
    }
}
