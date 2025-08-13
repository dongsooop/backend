package com.dongsoop.dongsoop.mailverify.service;

import com.dongsoop.dongsoop.mailverify.exception.UsingAllMailVerifyOpportunityException;
import com.dongsoop.dongsoop.mailverify.exception.VerifyMailCodeNotAvailableException;
import org.springframework.data.redis.core.RedisTemplate;

public abstract class MailVerifier {
    
    protected final String opportunityKey;
    protected final String codeKey;
    protected final RedisTemplate<String, Object> redisTemplate;
    protected final MailVerifyNumberGenerator mailVerifyService;

    protected MailVerifier(RedisTemplate<String, Object> redisTemplate,
                           MailVerifyNumberGenerator mailVerifyService,
                           String opportunityKey,
                           String codeKey) {
        this.redisTemplate = redisTemplate;
        this.mailVerifyService = mailVerifyService;
        this.opportunityKey = opportunityKey;
        this.codeKey = codeKey;
    }

    protected abstract String getVerifyCodeKeyPrefix();

    public void validateVerificationCode(String userEmail, String code) {
        String redisKey = mailVerifyService.getRedisKeyByHashed(getVerifyCodeKeyPrefix(), userEmail);
        Integer storedOpportunity = getTypedValueFromRedisHashAsType(redisKey, opportunityKey, Integer.class);
        if (storedOpportunity == null || storedOpportunity <= 0) {
            throw new UsingAllMailVerifyOpportunityException();
        }

        String storedCode = getTypedValueFromRedisHashAsType(redisKey, codeKey, String.class);
        if (storedCode == null || !storedCode.equals(code)) {
            redisTemplate.opsForHash()
                    .increment(redisKey, opportunityKey, -1);
            throw new VerifyMailCodeNotAvailableException();
        }

        redisTemplate.delete(redisKey);
    }

    protected <T> T getTypedValueFromRedisHashAsType(String redisKey, String hashKey, Class<T> type) {
        Object value = redisTemplate.opsForHash()
                .get(redisKey, hashKey);

        if (type.isInstance(value)) {
            return type.cast(value);
        }

        return null;
    }
}
