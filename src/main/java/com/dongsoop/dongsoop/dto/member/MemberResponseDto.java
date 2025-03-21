// MemberResponseDto.java
package com.dongsoop.dongsoop.dto.member;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberResponseDto {
    private Long id;
    private String email;
    private String nickname;
    private String studentNumber;
    private String department;
}