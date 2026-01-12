package com.dongsoop.dongsoop.oauth.provider;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.oauth.dto.AppleJwk;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.dto.SocialAccountLinkRequest;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccount;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccountId;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.exception.AlreadyLinkedSocialAccountException;
import com.dongsoop.dongsoop.oauth.exception.InvalidAppleTokenException;
import com.dongsoop.dongsoop.oauth.repository.MemberSocialAccountRepository;
import com.dongsoop.dongsoop.oauth.validator.MemberSocialAccountValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppleSocialProvider implements SocialProvider {

    private static final String SERVICE_NAME = "apple";
    private static final RestTemplate restTemplate = new RestTemplate();

    private final MemberSocialAccountValidator memberSocialAccountValidator;
    private final MemberRepository memberRepository;
    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final ObjectMapper objectMapper;

    @Value("${oauth.apple.issuer}")
    private String issuer;

    @Value("${spring.security.oauth2.client.provider.apple.user-name-attribute}")
    private String appleUserNameAttribute;

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String appleClientId;

    @Value("${spring.security.oauth2.client.provider.apple.jwk-set-uri}")
    private String jwtUri;

    public String serviceName() {
        return SERVICE_NAME;
    }

    @Override
    public String extractProviderId(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        return String.valueOf(attributes.get(appleUserNameAttribute));
    }

    @Override
    public Long login(String identityToken) {
        String providerId = this.getProviderId(identityToken);

        // 회원 검증
        MemberSocialAccountDto socialAccount = memberSocialAccountValidator.validate(providerId);
        return socialAccount.member().getId();
    }

    @Override
    public LocalDateTime linkSocialAccount(Long memberId, SocialAccountLinkRequest request) {
        String providerId = this.getProviderId(request.providerToken());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        MemberSocialAccountId socialAccountId = new MemberSocialAccountId(providerId, OAuthProviderType.APPLE);
        if (this.memberSocialAccountRepository.existsById(socialAccountId)) {
            throw new AlreadyLinkedSocialAccountException();
        }

        MemberSocialAccount socialAccount = new MemberSocialAccount(socialAccountId, member);
        MemberSocialAccount saved = this.memberSocialAccountRepository.save(socialAccount);

        return saved.getCreateAt();
    }

    private String getProviderId(String identityToken) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(jwtUri, Map.class);
            List<Map<String, Object>> keys = (List<Map<String, Object>>) response.getBody().get("keys");
            List<AppleJwk> appleJwkList = keys.stream()
                    .map(k -> objectMapper.convertValue(k, AppleJwk.class))
                    .toList();

            Claims claims = getClaims(identityToken, appleJwkList);
            return claims.getSubject();
        } catch (IllegalArgumentException | ExpiredJwtException e) {
            log.error("invalid apple identity token: {}", e.getMessage());
            throw new InvalidAppleTokenException();
        } catch (Exception e) {
            log.error("apple login error: {}", e.getMessage());
            throw new InvalidAppleTokenException();
        }
    }

    private PublicKey getApplePublicKey(AppleJwk appleJwk) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] nBytes = Base64.getUrlDecoder().decode(appleJwk.n());
        byte[] eBytes = Base64.getUrlDecoder().decode(appleJwk.e());

        // 바이트 배열을 BigInteger로 변환 (1은 양수를 의미).
        BigInteger nValue = new BigInteger(1, nBytes);
        BigInteger eValue = new BigInteger(1, eBytes);

        // RSA 공개키 스펙 생성
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(nValue, eValue);

        // KeyFactory를 통해 최종 PublicKey 객체 생성
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(publicKeySpec);
    }

    private Claims getClaims(String identityToken, List<AppleJwk> appleJwkList) {
        for (AppleJwk appleJwk : appleJwkList) {
            try {
                PublicKey applePublicKey = this.getApplePublicKey(appleJwk);

                return Jwts.parser()
                        .verifyWith(applePublicKey)
                        .requireIssuer(issuer)
                        .requireAudience(appleClientId)
                        .build()
                        .parseSignedClaims(identityToken)
                        .getPayload();
            } catch (NoSuchAlgorithmException | InvalidKeySpecException ignored) {
            }
        }

        throw new InvalidAppleTokenException();
    }
}
