package com.dongsoop.dongsoop.eclass.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EclassConfig {

    @Value("${eclass.connect-timeout-ms:5000}")
    private int connectTimeout;

    @Value("${eclass.read-timeout-ms:15000}")
    private int readTimeout;

    // 공용 restTemplate(4초)보다 긴 읽기 타임아웃이 필요하다 — 과제 전체 목록 응답이 수백 건에 이른다
    @Bean
    public RestTemplate eclassRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
