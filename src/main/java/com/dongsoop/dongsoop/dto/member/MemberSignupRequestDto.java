// MemberSignupRequestDto.java
package com.dongsoop.dongsoop.dto.member;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberSignupRequestDto {
    private String email;
    private String password;
    private String nickname;
    private String studentNumber;
    private String department;
}