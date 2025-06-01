package com.dongsoop.dongsoop.jwt.controller;

import com.dongsoop.dongsoop.jwt.dto.IssuedToken;
import com.dongsoop.dongsoop.jwt.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/token")
@RequiredArgsConstructor
public class JwtController {

    private final JwtService jwtService;

    @PostMapping("/reissue")
    public ResponseEntity<IssuedToken> reissueByRefreshToken(@RequestBody String refreshToken) {
        IssuedToken issuedToken = jwtService.issuedTokenByRefreshToken(refreshToken);

        return ResponseEntity.ok(issuedToken);
    }
}
