package com.dongsoop.dongsoop.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // 서비스 기준 시간대는 KST 다. 서버 기본 시간대에 의존하지 않는다
    @Bean
    public Clock clock() {
        return Clock.system(KST);
    }
}
