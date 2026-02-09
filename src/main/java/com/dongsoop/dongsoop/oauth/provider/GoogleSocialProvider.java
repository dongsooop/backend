package com.dongsoop.dongsoop.oauth.provider;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.dto.SocialAccountLinkRequest;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccount;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccountId;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.exception.InvalidGoogleTokenException;
import com.dongsoop.dongsoop.oauth.lock.SocialAccountLockManager;
import com.dongsoop.dongsoop.oauth.repository.MemberSocialAccountRepository;
import com.dongsoop.dongsoop.oauth.validator.MemberSocialAccountValidator;
import com.dongsoop.dongsoop.role.entity.Role;
import com.dongsoop.dongsoop.role.repository.MemberRoleRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
public class GoogleSocialProvider implements SocialProvider {

    private static final String SERVICE_NAME = "google";
    private static final String ATTRIBUTE_NAME = "sub";

    private final MemberSocialAccountValidator memberSocialAccountValidator;
    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final SocialAccountLockManager socialAccountLockManager;
    private final MemberRoleRepository memberRoleRepository;
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String googleUserInfoUrl;

    @Value("${spring.security.oauth2.client.provider.google.user-name-attribute}")
    private String googleUserNameAttribute;

    @Value("${oauth.google.revoke-uri}")
    private String revokeUri;

    public String serviceName() {
        return SERVICE_NAME;
    }

    @Override
    public String extractProviderId(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Object sub = attributes.get(ATTRIBUTE_NAME);
        if (sub == null) {
            throw new InvalidGoogleTokenException();
        }

        return sub.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public Authentication login(String providerToken) {
        String providerId = this.getProviderId(providerToken);

        MemberSocialAccountDto socialAccount = memberSocialAccountValidator.validate(providerId,
                OAuthProviderType.GOOGLE);

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
        socialAccountLockManager.lockBySocialAccount(providerId); // 소셜 계정 식별자 락 획득
        try {
            MemberSocialAccountId socialAccountId = new MemberSocialAccountId(providerId, OAuthProviderType.GOOGLE);
            // 이미 소셜 계정이 가입되었는지, 회원이 해당 프로바이더 타입으로 연동했는지 검증
            this.memberSocialAccountValidator.validateAlreadyLinked(socialAccountId, member, OAuthProviderType.GOOGLE);

            MemberSocialAccount socialAccount = new MemberSocialAccount(socialAccountId, member);
            MemberSocialAccount saved = this.memberSocialAccountRepository.save(socialAccount);
            return saved.getCreatedAt();
        } finally {
            // 작업 후 락 해제
            socialAccountLockManager.unlockBySocialAccount(providerId);
        }
    }

    private String getProviderId(String providerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(providerToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    googleUserInfoUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null || body.get(googleUserNameAttribute) == null) {
                throw new InvalidGoogleTokenException();
            }

            return body.get(googleUserNameAttribute).toString();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.info("Google token validation failed: {}", e.getMessage());
            throw new InvalidGoogleTokenException();
        }
    }

    @Override
    public void revoke(String providerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("token", providerToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            restTemplate.postForEntity(revokeUri, request, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.warn("Google revoke failed: {}", e.getMessage());
            throw new InvalidGoogleTokenException();
        }
    }
}
