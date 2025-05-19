package com.dongsoop.dongsoop.member.dto;

import com.dongsoop.dongsoop.jwt.dto.TokenIssueResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginDetails {

    private LoginMemberDetails loginMemberDetail;

    private TokenIssueResponse issuedToken;
}
