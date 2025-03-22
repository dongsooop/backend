package com.dongsoop.dongsoop.jwt;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtKeyManager {

    @Value("${jwt.secret-key}")
    private String secretKey;

    private SecretKey key;

    @PostConstruct
    private void init() {
        this.key = createSecretKey();
    }

    protected SecretKey getSecretKey() {
        return this.key;
    }

    private SecretKey createSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
