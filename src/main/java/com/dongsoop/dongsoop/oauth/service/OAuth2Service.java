package com.dongsoop.dongsoop.oauth.service;

import com.dongsoop.dongsoop.member.dto.LoginResponse;

public interface OAuth2Service {

    LoginResponse acceptLogin(Long memberId);
}
