package com.dongsoop.dongsoop.oauth.provider;

import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.exception.TokenExpiredException;
import com.dongsoop.dongsoop.jwt.exception.TokenMalformedException;
import com.dongsoop.dongsoop.jwt.exception.TokenSignatureException;
import com.dongsoop.dongsoop.jwt.exception.TokenUnsupportedException;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final JwtUtil jwtUtil;

    @Value("${oauth.apple.issuer}")
    private String issuer;

    @Value("${spring.security.oauth2.client.provider.apple.user-name-attribute}")
    private String appleUserNameAttribute;

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String appleClientId;

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
        String providerId = this.getProviderId(request.providerToken());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        MemberSocialAccountId socialAccountId = new MemberSocialAccountId(providerId, OAuthProviderType.APPLE);
        MemberSocialAccount socialAccount = new MemberSocialAccount(socialAccountId, member);

        try {
            MemberSocialAccount saved = this.memberSocialAccountRepository.save(socialAccount);
            return saved.getCreatedAt();

        } catch (DataIntegrityViolationException e) {
            log.info("DataIntegrityViolationException occurred while linking Apple social account: {}", e.getMessage());

            // 이미 해당 소셜 타입으로 연동된 적이 있는지 확인
            if (this.memberSocialAccountRepository.existsById(socialAccountId)) {
                throw new AlreadyLinkedSocialAccountException();
            }

            // 소셜 계정이 다른 회원과 연동된 적이 있는 경우
            throw new AlreadyLinkedProviderTypeException();
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
            String headerJson = new String(decodedHeader);

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
