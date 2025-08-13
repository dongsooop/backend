package com.dongsoop.dongsoop.mailverify.passwordupdate;

import com.dongsoop.dongsoop.mailverify.service.MailVerifier;
import com.dongsoop.dongsoop.mailverify.service.MailVerifyNumberGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PasswordUpdateMailValidator extends MailVerifier {

    private final String verifyCodeKeyPrefix;

    public PasswordUpdateMailValidator(RedisTemplate<String, Object> redisTemplate,
                                       MailVerifyNumberGenerator mailVerifyService,
                                       @Value("${mail.redis.key.opportunity}") String opportunityKey,
                                       @Value("${mail.redis.key.code}") String codeKey,
                                       @Value("${mail.verify.password-update.redis.prefix}") String verifyCodeKeyPrefix) {
        super(redisTemplate, mailVerifyService, opportunityKey, codeKey);
        this.verifyCodeKeyPrefix = verifyCodeKeyPrefix;
    }

    @Override
    protected String getVerifyCodeKeyPrefix() {
        return verifyCodeKeyPrefix;
    }
}
