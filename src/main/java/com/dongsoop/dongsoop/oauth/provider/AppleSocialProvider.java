package com.dongsoop.dongsoop.oauth.provider;

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
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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
    private final MemberRoleRepository memberRoleRepository;
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
    public LocalDateTime linkSocialAccount(Long memberId, SocialAccountLinkRequest request) {
        String providerId = this.getProviderId(request.providerToken());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        MemberSocialAccountId socialAccountId = new MemberSocialAccountId(providerId, OAuthProviderType.APPLE);
        // 이미 연동된 소셜 계정인지 확인
        if (this.memberSocialAccountRepository.existsById(socialAccountId)) {
            throw new AlreadyLinkedSocialAccountException();
        }

        // 이미 회원이 해당 소셜 타입을 연동한 적이 있는지 확인
        this.memberSocialAccountRepository.findByMemberAndProviderType(member, OAuthProviderType.APPLE)
                .ifPresent((m) -> {
                    throw new AlreadyLinkedProviderTypeException();
                });

        MemberSocialAccount socialAccount = new MemberSocialAccount(socialAccountId, member);
        MemberSocialAccount saved = this.memberSocialAccountRepository.save(socialAccount);

        return saved.getCreatedAt();
    }

    private String getProviderId(String identityToken) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(jwtUri, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                log.error("failed to fetch apple jwk: response body is null");
                throw new InvalidAppleTokenException();
            }

            List<Map<String, Object>> keys = (List<Map<String, Object>>) responseBody.get("keys");
            Map<String, AppleJwk> appleJwkMap = keys.stream()
                    .map(k -> this.objectMapper.convertValue(k, AppleJwk.class))
                    .collect(Collectors.toMap(AppleJwk::kid, jwk -> jwk, (first, second) -> first));

            String kid = this.extractKidFromToken(identityToken);
            AppleJwk appleJwk = appleJwkMap.get(kid);
            if (appleJwk == null) {
                log.error("apple jwk not found for kid: {}", kid);
                throw new InvalidAppleTokenException();
            }

            Claims claims = this.getClaims(identityToken, appleJwk);
            return claims.getSubject();
        } catch (IllegalArgumentException | ExpiredJwtException e) {
            log.error("invalid apple identity token: {}", e.getMessage());
            throw new InvalidAppleTokenException();
        } catch (Exception e) {
            log.error("apple login error: {}", e.getMessage());
            throw new InvalidAppleTokenException();
        }
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

    private Claims getClaims(String identityToken, AppleJwk appleJwk) {
        try {
            PublicKey applePublicKey = this.getApplePublicKey(appleJwk);

            return Jwts.parser()
                    .verifyWith(applePublicKey)
                    .requireIssuer(issuer)
                    .requireAudience(appleClientId)
                    .build()
                    .parseSignedClaims(identityToken)
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

            return (String) headerMap.get("kid");
        } catch (JsonProcessingException e) {
            log.info("invalid apple identity token: {}", e.getMessage());
            throw new InvalidAppleTokenException();
        }
    }
}
