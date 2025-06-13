package com.dongsoop.dongsoop.member.service;

import com.dongsoop.dongsoop.member.dto.LoginAuthenticate;
import com.dongsoop.dongsoop.member.dto.LoginDetails;
import com.dongsoop.dongsoop.member.dto.LoginRequest;
import com.dongsoop.dongsoop.member.dto.SignupRequest;
import com.dongsoop.dongsoop.member.entity.Member;

public interface MemberService {

    void signup(SignupRequest request);

    LoginDetails login(LoginRequest loginRequest);

    LoginAuthenticate getLoginAuthenticateByNickname(String nickname);

    Member getMemberReferenceByContext();

    String getNicknameById(Long userId);

    Long getMemberIdByAuthentication();
}
