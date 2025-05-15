package com.dongsoop.dongsoop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // JpaAuditingConfig를 분리한 이유는 Application 클래스의 책임을 줄이기 위함이다.
    // 책임을 줄여 Test 환경에서 JpaAuditingConfig를 사용하지 않도록 설정할 수 있다.
}
