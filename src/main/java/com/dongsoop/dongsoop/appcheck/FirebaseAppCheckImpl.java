package com.dongsoop.dongsoop.appcheck;

import com.dongsoop.dongsoop.appcheck.dto.FirebaseDeviceData;
import com.dongsoop.dongsoop.appcheck.dto.FirebaseKeys;
import com.dongsoop.dongsoop.appcheck.exception.UnknownFirebaseFetchJWKException;
import com.dongsoop.dongsoop.jwt.exception.TokenNotFoundException;
import com.dongsoop.dongsoop.jwt.exception.TokenSignatureException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.micrometer.common.util.StringUtils;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class FirebaseAppCheckImpl implements FirebaseAppCheck {

    private static final String AUDIENCE_PREFIX = "projects/";
    private static final String JWKS_URL = "https://firebaseappcheck.googleapis.com/v1/jwks";
    private static final String JWKS_ISSUER = "https://firebaseappcheck.googleapis.com/";

    private Map<String, FirebaseDeviceData> cache = new ConcurrentHashMap<>();

    @Value("${firebase.project.number}")
    private String projectNumber;

    @Override
    public void validate(String token) {
        if (token == null || StringUtils.isBlank(token)) {
            throw new TokenNotFoundException();
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            String header = token.split("\\.")[0];
            String decoded = new String(Base64.getUrlDecoder()
                    .decode(header));

            FirebaseDeviceData requesterData = mapper.readValue(decoded, FirebaseDeviceData.class);
            String kid = requesterData.getKid();

            FirebaseDeviceData data = cache.get(kid);

            byte[] modulusBytes = Base64.getUrlDecoder().decode(data.getN());
            byte[] exponentBytes = Base64.getUrlDecoder().decode(data.getE());
            BigInteger modulus = new BigInteger(1, modulusBytes);
            BigInteger exponent = new BigInteger(1, exponentBytes);
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(publicKeySpec);

            cache.getOrDefault(kid, null);
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 발급자(issuer) 확인
            String issuer = claims.getIssuer();
            if (!issuer.equals(JWKS_ISSUER + projectNumber)) {
                throw new SecurityException("Invalid issuer");
            }

            // 대상(audience) 확인
            Set<String> audience = claims.getAudience();
            if (!audience.contains(AUDIENCE_PREFIX + projectNumber)) {
                throw new TokenSignatureException();
            }
        } catch (Exception e) {
            throw new UnknownFirebaseFetchJWKException(e);
        }
    }

    public void updateCache() throws IOException, InterruptedException {
        HttpRequest firebaseRequest = HttpRequest.newBuilder()
                .uri(URI.create(JWKS_URL))
                .GET()
                .build();

        // 2. HTTP 요청 전송 및 응답 수신
        HttpResponse<String> firebaseResponse = HttpClient.newHttpClient()
                .send(firebaseRequest, HttpResponse.BodyHandlers.ofString());

        if (firebaseResponse.statusCode() != HttpStatus.OK.value()) {
            throw new UnknownFirebaseFetchJWKException();
        }

        ObjectMapper mapper = new ObjectMapper();
        FirebaseKeys firebaseKeys = mapper.readValue(firebaseResponse.body(), FirebaseKeys.class);
        cache = firebaseKeys.getKeys().stream()
                .collect(Collectors.toMap(data -> data.getKid(), data -> data));
    }
}
