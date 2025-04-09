package com.dongsoop.dongsoop.jwt;

import com.dongsoop.dongsoop.jwt.dto.AuthenticationInformationByToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.claims.name}")
    private String roleClaimName;

    private final JwtKeyManager jwtKeyManager;

    protected Claims getClaims(String token) {
        SecretKey key = jwtKeyManager.getSecretKey();

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    protected String issue(Date tokenExpiredTime, String name, List<String> roleList) {
        SecretKey key = jwtKeyManager.getSecretKey();

        return Jwts.builder()
                .subject(name)
                .claim(roleClaimName, roleList)
                .signWith(key)
                .expiration(tokenExpiredTime)
                .compact();
    }

    public AuthenticationInformationByToken getTokenInformation(String token) {
        Claims claims = getClaims(token);

        Long id = Long.parseLong(claims.getSubject());
        List<?> roleCollection = claims.get(roleClaimName,
                List.class);

        List<? extends GrantedAuthority> roles = roleCollection.stream()
                .filter(String.class::isInstance)
                .map(String::valueOf)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new AuthenticationInformationByToken(id, roles);
    }

}
