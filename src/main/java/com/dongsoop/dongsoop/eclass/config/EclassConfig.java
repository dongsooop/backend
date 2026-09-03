package com.dongsoop.dongsoop.eclass.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EclassConfig {

    @Value("${eclass.token-key}")
    private String tokenKey;

    @Value("${eclass.token-salt}")
    private String tokenSalt;

    @Value("${eclass.connect-timeout-ms:5000}")
    private int connectTimeout;

    @Value("${eclass.read-timeout-ms:15000}")
    private int readTimeout;

    /**
     * 이클래스 토큰을 저장할 때 쓰는 암호화기. AES-256-GCM에 매번 새로운 IV를 쓴다.
     *
     * <p>암호화는 직접 구현하지 않는다 — {@code spring-boot-starter-security}가 이미 끌고 오는
     * 구현을 쓴다. 키는 password와 salt에서 유도하며, salt는 비밀이 아니라 설정 파일에 두고
     * password만 환경변수로 받는다.
     */
    @Bean
    public TextEncryptor eclassTokenEncryptor() {
        return Encryptors.delux(tokenKey, tokenSalt);
    }

    // 공용 restTemplate(4초)보다 긴 읽기 타임아웃이 필요하다 — 과제 전체 목록 응답이 수백 건에 이른다
    @Bean
    public RestTemplate eclassRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }
}
