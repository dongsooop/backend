package com.dongsoop.dongsoop.member.service;

import com.dongsoop.dongsoop.member.dto.LoginDetails;
import com.dongsoop.dongsoop.member.dto.LoginRequest;
import com.dongsoop.dongsoop.member.dto.SignupRequest;
import com.dongsoop.dongsoop.member.dto.UpdatePasswordRequest;
import com.dongsoop.dongsoop.member.entity.Member;

public interface MemberService {

    void signup(SignupRequest request);

    LoginDetails login(LoginRequest loginRequest);

    Member getMemberReferenceByContext();

    String getNicknameById(Long userId);

    Long getMemberIdByAuthentication();

    void deleteMember();

    void updatePassword(UpdatePasswordRequest request);
}
