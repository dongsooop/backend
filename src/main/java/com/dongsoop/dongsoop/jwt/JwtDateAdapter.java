package com.dongsoop.dongsoop.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import java.time.Instant;
import java.util.Date;

/** JJWT의 legacy {@link Date} API와 애플리케이션의 {@link Instant} 경계를 한곳에서 변환한다. */
public final class JwtDateAdapter {

    private JwtDateAdapter() {
    }

    public static JwtBuilder applyTimestamps(JwtBuilder builder, Instant issuedAt, Instant expiresAt) {
        return builder.issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt));
    }

    public static Instant getIssuedAt(Claims claims) {
        Date issuedAt = claims.getIssuedAt();
        return issuedAt == null ? null : issuedAt.toInstant();
    }
}
