package com.dongsoop.dongsoop.jwt;

import com.dongsoop.dongsoop.jwt.dto.AuthenticationInformationByToken;
import com.dongsoop.dongsoop.jwt.exception.TokenExpiredException;
import com.dongsoop.dongsoop.jwt.exception.TokenMalformedException;
import com.dongsoop.dongsoop.jwt.exception.TokenSignatureException;
import com.dongsoop.dongsoop.jwt.exception.TokenUnsupportedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtKeyManager jwtKeyManager;

    @Value("${jwt.claims.role.name}")
    private String roleClaimName;

    @Value("${jwt.claims.type.name}")
    private String typeClaimName;

    public Claims getClaims(String token) {
        SecretKey key = jwtKeyManager.getSecretKey();

        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException(e);
        } catch (MalformedJwtException | IllegalArgumentException e) {
            throw new TokenMalformedException(e);
        } catch (SignatureException e) {
            throw new TokenSignatureException(e);
        } catch (UnsupportedJwtException e) {
            throw new TokenUnsupportedException(e);
        }
    }

    protected String issue(Date tokenExpiredTime, String id, List<String> roleList, JWTType type) {
        SecretKey key = jwtKeyManager.getSecretKey();

        return Jwts.builder()
                .subject(id)
                .claim(roleClaimName, roleList)
                .claim(typeClaimName, type.name())
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

    public Authentication getAuthenticationByToken(String token) {
        AuthenticationInformationByToken authenticationInformation = getTokenInformation(token);
        Long id = authenticationInformation.getId();
        Collection<? extends GrantedAuthority> role = authenticationInformation.getRole();

        return new UsernamePasswordAuthenticationToken(id, null, role);
    }
}
