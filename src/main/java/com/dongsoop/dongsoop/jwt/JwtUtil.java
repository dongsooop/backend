package com.dongsoop.dongsoop.jwt;

import com.dongsoop.dongsoop.jwt.dto.AuthenticationInformationByToken;
import com.dongsoop.dongsoop.jwt.exception.TokenExpiredException;
import com.dongsoop.dongsoop.jwt.exception.TokenMalformedException;
import com.dongsoop.dongsoop.jwt.exception.TokenSignatureException;
import com.dongsoop.dongsoop.jwt.exception.TokenUnsupportedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import java.security.PublicKey;
import java.time.Instant;
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

    public Claims getClaims(String jwt, PublicKey publicKey, String issuer,
                            String audience) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(jwt)
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

    public static final String DEVICE_ID_CLAIM = "did";

    // JJWT의 issuedAt/expiration API가 Date를 요구하므로 이 경계에서만 변환한다.
    @SuppressWarnings("java:S2143")
    protected String issue(Instant tokenExpiredTime, String id, List<String> roleList, JWTType type, Long deviceId) {
        SecretKey key = jwtKeyManager.getSecretKey();

        JwtBuilder builder = Jwts.builder()
                .subject(id)
                .claim(roleClaimName, roleList)
                .claim(typeClaimName, type.name())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(tokenExpiredTime))
                .signWith(key);

        if (deviceId != null) {
            builder.claim(DEVICE_ID_CLAIM, deviceId);
        }

        return builder.compact();
    }

    // JJWT의 발급 시각 반환 타입은 Date이며, 애플리케이션에는 Instant를 전달한다.
    @SuppressWarnings("java:S2143")
    public static Instant getIssuedAt(Claims claims) {
        Date issuedAt = claims.getIssuedAt();
        if (issuedAt == null) {
            return null;
        }
        return issuedAt.toInstant();
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
