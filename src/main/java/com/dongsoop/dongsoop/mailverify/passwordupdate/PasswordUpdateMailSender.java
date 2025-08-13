package com.dongsoop.dongsoop.mailverify.passwordupdate;

import com.dongsoop.dongsoop.mailverify.mailgenerator.MailTextGenerator;
import com.dongsoop.dongsoop.mailverify.service.MailVerifyNumberGenerator;
import com.dongsoop.dongsoop.mailverify.service.VerifyMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class PasswordUpdateMailSender extends VerifyMailSender {


    private static final int VERIFY_CODE_EXPIRATION_TIME = 300; // 5 minute
    private static final String SUBJECT = "[동숲] 인증번호 발송";

    private final String verifyCodeKeyPrefix;

    public PasswordUpdateMailSender(RedisTemplate<String, Object> redisTemplate,
                                    JavaMailSender mailSender,
                                    MailVerifyNumberGenerator mailVerifyNumberGenerator,
                                    MailTextGenerator mailTextGenerator,
                                    @Value("${mail.redis.key.opportunity}") String opportunityKey,
                                    @Value("${mail.redis.key.code}") String codeKey,
                                    @Value("${mail.redis.key.success}") String successKey,
                                    @Value("${mail.verify.password-update.redis.prefix}") String verifyCodeKeyPrefix) {
        super(redisTemplate, mailSender, mailVerifyNumberGenerator, mailTextGenerator, opportunityKey, codeKey,
                successKey);
        this.verifyCodeKeyPrefix = verifyCodeKeyPrefix;
    }

    protected String getSubject() {
        return SUBJECT;
    }

    protected int getOpportunityCount() {
        return DEFAULT_OPPORTUNITY_COUNT;
    }

    @Override
    protected int getCodeLength() {
        return DEFAULT_CODE_LENGTH;
    }

    @Override
    protected String getVerifyCodeKeyPrefix() {
        return verifyCodeKeyPrefix;
    }

    @Override
    protected Integer getVerifyCodeExpirationTime() {
        return VERIFY_CODE_EXPIRATION_TIME;
    }
}
