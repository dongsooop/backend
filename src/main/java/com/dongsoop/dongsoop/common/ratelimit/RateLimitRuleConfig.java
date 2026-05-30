package com.dongsoop.dongsoop.common.ratelimit;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * Rate Limit 규칙 정의. 인증/이메일/검색 등 남용 위험이 높은 엔드포인트를 보호한다.
 *
 * <p>한도/시간창 조정 시 이 클래스만 수정하면 된다. (향후 외부 설정으로 이관 가능)
 */
@Configuration
public class RateLimitRuleConfig {

    @Bean
    public RateLimitRules rateLimitRules() {
        return new RateLimitRules(List.of(
                new RateLimitRule(
                        "login",
                        HttpMethod.POST,
                        List.of("/member/login", "/oauth2/kakao", "/oauth2/google", "/oauth2/apple"),
                        5,
                        Duration.ofMinutes(1)),
                new RateLimitRule(
                        "email",
                        HttpMethod.POST,
                        List.of("/mail-verify/send", "/mail-verify/send/password-update"),
                        5,
                        Duration.ofMinutes(1)),
                new RateLimitRule(
                        "search",
                        HttpMethod.GET,
                        List.of("/search/**"),
                        30,
                        Duration.ofMinutes(1))));
    }

    /**
     * RateLimitFilter는 SecurityFilterChain에서만 실행되도록 하고, 서블릿 컨테이너의 자동 등록은 비활성화한다.
     * (자동 등록 시 보안 체인과 합쳐 요청당 2회 실행되어 카운트가 중복된다.)
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter rateLimitFilter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }
}
