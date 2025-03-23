package com.dongsoop.dongsoop.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {

    private String email;
    private String password;
    private String nickname;
    private String studentId;
    private String department;
}