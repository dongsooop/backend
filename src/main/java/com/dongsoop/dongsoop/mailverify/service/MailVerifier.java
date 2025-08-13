package com.dongsoop.dongsoop.mailverify.service;

import com.dongsoop.dongsoop.mailverify.exception.UsingAllMailVerifyOpportunityException;
import com.dongsoop.dongsoop.mailverify.exception.VerifyMailCodeNotAvailableException;
import org.springframework.data.redis.core.RedisTemplate;

public abstract class MailVerifier {

    protected final String opportunityKey;
    protected final String codeKey;
    protected final String successKey;
    protected final RedisTemplate<String, Object> redisTemplate;
    protected final MailVerifyNumberGenerator mailVerifyNumberGenerator;

    protected MailVerifier(RedisTemplate<String, Object> redisTemplate,
                           MailVerifyNumberGenerator mailVerifyNumberGenerator,
                           String opportunityKey,
                           String codeKey,
                           String successKey) {
        this.redisTemplate = redisTemplate;
        this.mailVerifyNumberGenerator = mailVerifyNumberGenerator;
        this.opportunityKey = opportunityKey;
        this.codeKey = codeKey;
        this.successKey = successKey;
    }

    protected abstract String getVerifyCodeKeyPrefix();

    public void validateVerificationCodeWithOpportunity(String userEmail, String code) {
        String redisKey = mailVerifyNumberGenerator.getRedisKeyByHashed(getVerifyCodeKeyPrefix(), userEmail);
        validateOpportunity(redisKey);

        String storedCode = getTypedValueFromRedisHashAsType(redisKey, codeKey, String.class);
        if (storedCode == null || !storedCode.equals(code)) {
            redisTemplate.opsForHash()
                    .increment(redisKey, opportunityKey, -1);
            throw new VerifyMailCodeNotAvailableException();
        }

        redisTemplate.opsForHash()
                .put(redisKey, successKey, true);
    }

    private void validateOpportunity(String redisKey) {
        Integer storedOpportunity = getTypedValueFromRedisHashAsType(redisKey, opportunityKey, Integer.class);
        if (storedOpportunity == null || storedOpportunity <= 0) {
            throw new UsingAllMailVerifyOpportunityException();
        }
    }

    protected <T> T getTypedValueFromRedisHashAsType(String redisKey, String hashKey, Class<T> type) {
        Object value = redisTemplate.opsForHash()
                .get(redisKey, hashKey);

        if (type.isInstance(value)) {
            return type.cast(value);
        }

        return null;
    }

    public void removeVerificationCode(String userEmail) {
        String redisKey = mailVerifyNumberGenerator.getRedisKeyByHashed(getVerifyCodeKeyPrefix(), userEmail);
        redisTemplate.delete(redisKey);
    }

    public void validateVerifySuccess(String email) {
        String redisKey = mailVerifyNumberGenerator.getRedisKeyByHashed(getVerifyCodeKeyPrefix(), email);
        Boolean isSuccess = getTypedValueFromRedisHashAsType(redisKey, successKey, Boolean.class);

        if (isSuccess == null || !isSuccess) {
            throw new VerifyMailCodeNotAvailableException();
        }
    }
}
