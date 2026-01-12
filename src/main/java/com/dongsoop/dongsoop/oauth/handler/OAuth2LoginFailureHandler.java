package com.dongsoop.dongsoop.oauth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${oauth.failure-redirect-uri}")
    private String failureRedirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // 에러 메시지와 함께 로그인 페이지로 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(failureRedirectUri)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
        log.info("OAuth2 login failed. redirect to: {} cause: {}", targetUrl, exception.getMessage());
    }
}
