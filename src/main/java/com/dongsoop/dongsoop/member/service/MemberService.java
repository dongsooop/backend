package com.dongsoop.dongsoop.member.service;

import com.dongsoop.dongsoop.jwt.dto.TokenIssueResponse;
import com.dongsoop.dongsoop.member.dto.LoginAuthenticate;
import com.dongsoop.dongsoop.member.dto.LoginRequest;
import com.dongsoop.dongsoop.member.dto.SignupRequest;
import com.dongsoop.dongsoop.member.entity.Member;

public interface MemberService {

    void signup(SignupRequest request);

    TokenIssueResponse login(LoginRequest loginRequest);

    LoginAuthenticate getLoginAuthenticateByNickname(String nickname);

    Member getMemberReferenceByContext();
}
