package com.dongsoop.dongsoop.oauth.exception;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

public class AccountNotLinkedException extends OAuth2AuthenticationException {

    public AccountNotLinkedException() {
        super("소셜 계정이 연결되어 있지 않습니다: ");
    }
}
