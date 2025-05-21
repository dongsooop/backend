package com.dongsoop.dongsoop.jwt.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IssuedToken {
    private String accessToken;
    private String refreshToken;
}