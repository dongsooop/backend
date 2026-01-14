package com.dongsoop.dongsoop.oauth.provider;

import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.exception.TokenExpiredException;
import com.dongsoop.dongsoop.jwt.exception.TokenMalformedException;
import com.dongsoop.dongsoop.jwt.exception.TokenSignatureException;
import com.dongsoop.dongsoop.jwt.exception.TokenUnsupportedException;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.oauth.dto.AppleJwk;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.dto.SocialAccountLinkRequest;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccount;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccountId;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.exception.AlreadyLinkedProviderTypeException;
import com.dongsoop.dongsoop.oauth.exception.AlreadyLinkedSocialAccountException;
import com.dongsoop.dongsoop.oauth.exception.InvalidAppleTokenException;
import com.dongsoop.dongsoop.oauth.repository.MemberSocialAccountRepository;
import com.dongsoop.dongsoop.oauth.validator.MemberSocialAccountValidator;
import com.dongsoop.dongsoop.role.entity.Role;
import com.dongsoop.dongsoop.role.repository.MemberRoleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppleSocialProvider implements SocialProvider {

    private static final String SERVICE_NAME = "apple";

    private final MemberSocialAccountValidator memberSocialAccountValidator;
    private final MemberRepository memberRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final ObjectMapper objectMapper;
    private final AppleJwkProvider appleJwkProvider;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;

    @Value("${oauth.apple.issuer}")
    private String issuer;

    @Value("${spring.security.oauth2.client.provider.apple.user-name-attribute}")
    private String appleUserNameAttribute;

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String appleClientId;

    @Value("${oauth.apple.revoke-uri}")
    private String revokeUri;

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.apple.client-secret}")
    private String clientSecret;

    public String serviceName() {
        return SERVICE_NAME;
    }

    @Override
    public String extractProviderId(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Object sub = attributes.get(appleUserNameAttribute);
        if (sub == null) {
            throw new InvalidAppleTokenException();
        }

        return sub.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public Authentication login(String identityToken) {
        String providerId = this.getProviderId(identityToken);

        // 회원 검증
        MemberSocialAccountDto socialAccount = memberSocialAccountValidator.validate(providerId,
                OAuthProviderType.APPLE);

        Long memberId = socialAccount.member().getId();

        List<Role> allRoleByMemberId = memberRoleRepository.findAllByMemberId(memberId);

        Collection<? extends GrantedAuthority> roles = allRoleByMemberId.stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleType().name()))
                .toList();

        return new UsernamePasswordAuthenticationToken(memberId, null, roles);
    }

    @Override
    @Transactional
    public LocalDateTime linkSocialAccount(Long memberId, SocialAccountLinkRequest request) {
        // 락 획득
        Member member = this.memberSocialAccountRepository.findAndLockMember(memberId);

        String providerId = this.getProviderId(request.providerToken());
        MemberSocialAccountId socialAccountId = new MemberSocialAccountId(providerId, OAuthProviderType.APPLE);
        MemberSocialAccount socialAccount = new MemberSocialAccount(socialAccountId, member);

        try {
            MemberSocialAccount saved = this.memberSocialAccountRepository.save(socialAccount);
            return saved.getCreatedAt();

        } catch (DataIntegrityViolationException e) {
            // 소셜 계정이 이미 DB에 저장된 상태인 경우
            if (this.memberSocialAccountRepository.existsById(socialAccountId)) {
                throw new AlreadyLinkedSocialAccountException();
            }

            // 회원이 이미 동일한 ProviderType의 소셜 계정과 연동된 경우
            throw new AlreadyLinkedProviderTypeException();
        }
    }

    @Override
    public void revoke(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Parameters 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", this.clientId);
        params.add("client_secret", this.clientSecret);
        params.add("token", refreshToken);
        params.add("token_type_hint", "refresh_token"); // 선택사항이지만 권장

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    this.revokeUri,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Apple revoked successfully.");
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new InvalidAppleTokenException();
        }
    }

    private String getProviderId(String identityToken) {
        try {
            String kid = this.extractKidFromToken(identityToken);
            AppleJwk appleJwk = getJwk(kid);
            PublicKey publicKey = this.getApplePublicKey(appleJwk);
            Claims claims = this.jwtUtil.getClaims(identityToken, publicKey, issuer, appleClientId);

            return claims.getSubject();

        } catch (IllegalArgumentException e) {
            log.error("invalid apple identity token: {}", e.getMessage());
            throw new InvalidAppleTokenException();

        } catch (TokenExpiredException | TokenMalformedException | TokenSignatureException |
                 TokenUnsupportedException | InvalidAppleTokenException e) {
            throw e;

        } catch (Exception e) {
            log.error("apple login error: {}", e.getMessage());
            throw new InvalidAppleTokenException();
        }
    }

    private AppleJwk getJwk(String kid) {
        Map<String, AppleJwk> appleJwkMap = this.appleJwkProvider.getAppleJwkMap();
        AppleJwk appleJwk = appleJwkMap.get(kid);

        // 1차 시도 성공 시 반환
        if (appleJwk != null) {
            return appleJwk;
        }

        // 2차 시도: 캐시 제거 후 재조회
        this.appleJwkProvider.evictAppleJwkCache(); // 캐시 제거

        appleJwkMap = this.appleJwkProvider.getAppleJwkMap();

        appleJwk = appleJwkMap.get(kid);
        if (appleJwk == null) {
            log.error("apple jwk not found for kid: {}", kid);
            throw new InvalidAppleTokenException();
        }

        return appleJwk;
    }

    private PublicKey getApplePublicKey(AppleJwk appleJwk) {
        try {
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
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("failed to generate apple public key: {}", e.getMessage());
            throw new OAuth2AuthenticationException("애플 공개키 생성에 실패했습니다.");
        }
    }

    private String extractKidFromToken(String identityToken) {
        try {
            String[] parts = identityToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }

            // Base64URL 디코딩
            byte[] decodedHeader = Base64.getUrlDecoder().decode(parts[0]);
            String headerJson = new String(decodedHeader, StandardCharsets.UTF_8);

            Map<String, Object> headerMap = this.objectMapper.readValue(headerJson, Map.class);

            String kid = (String) headerMap.get("kid");
            if (kid == null) {
                log.info("kid not found in apple identity token header");
                throw new InvalidAppleTokenException();
            }

            return kid;
        } catch (JsonProcessingException e) {
            log.info("invalid apple identity token: {}", e.getMessage());
            throw new InvalidAppleTokenException();
        }
    }
}
