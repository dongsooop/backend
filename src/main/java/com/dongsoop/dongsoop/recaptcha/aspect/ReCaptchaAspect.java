package com.dongsoop.dongsoop.recaptcha.aspect;

import com.dongsoop.dongsoop.recaptcha.annotation.ReCaptchaRequired;
import com.dongsoop.dongsoop.recaptcha.exception.ReCaptchaTokenNotFoundException;
import com.dongsoop.dongsoop.recaptcha.service.ReCaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ReCaptchaAspect {

    private static final String RECAPTCHA_HEADER = "X-Recaptcha-Token";

    private final ReCaptchaService reCaptchaService;

    @Around("@annotation(reCaptchaRequired)")
    public Object around(ProceedingJoinPoint pjp, ReCaptchaRequired reCaptchaRequired) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String token = request.getHeader(RECAPTCHA_HEADER);

        if (!StringUtils.hasText(token)) {
            throw new ReCaptchaTokenNotFoundException();
        }

        reCaptchaService.verify(token, reCaptchaRequired.action());

        return pjp.proceed();
    }
}
