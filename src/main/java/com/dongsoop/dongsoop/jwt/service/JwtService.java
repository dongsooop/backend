package com.dongsoop.dongsoop.jwt.service;

import com.dongsoop.dongsoop.jwt.dto.IssuedToken;

public interface JwtService {

    IssuedToken issuedTokenByRefreshToken(String refreshToken);
}
