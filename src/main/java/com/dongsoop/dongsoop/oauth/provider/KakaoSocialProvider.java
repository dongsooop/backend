package com.dongsoop.dongsoop.oauth.provider;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.dto.SocialAccountLinkRequest;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccount;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccountId;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.exception.AlreadyLinkedProviderTypeException;
import com.dongsoop.dongsoop.oauth.exception.AlreadyLinkedSocialAccountException;
import com.dongsoop.dongsoop.oauth.exception.InvalidKakaoTokenException;
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
import org.springframework.dao.DataIntegrityViolationException;
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
public class KakaoSocialProvider implements SocialProvider {

    private static final String SERVICE_NAME = "kakao";
    private static final String ATTRIBUTE_NAME = "id";

    private final MemberSocialAccountValidator memberSocialAccountValidator;
    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final MemberRepository memberRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String kakaoUserInfoUrl;

    @Value("${oauth.kakao.user-name-attribute}")
    private String kakaoUserNameAttribute;

    @Value("${oauth.kakao.revoke-uri}")
    private String revokeUri;

    @Value("${oauth.kakao.target-id-type}")
    private String targetIdType;

    @Value("${oauth.kakao.mobile-token-value}")
    private String mobileTokenValue;

    public String serviceName() {
        return SERVICE_NAME;
    }

    @Override
    public String extractProviderId(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Object sub = attributes.get(ATTRIBUTE_NAME);
        if (sub == null) {
            throw new InvalidKakaoTokenException();
        }

        return sub.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public Authentication login(String providerToken) {
        String providerId = this.getProviderId(providerToken);

        // 회원 검증
        MemberSocialAccountDto socialAccount = memberSocialAccountValidator.validate(providerId,
                OAuthProviderType.KAKAO);

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
        MemberSocialAccountId socialAccountId = new MemberSocialAccountId(providerId, OAuthProviderType.KAKAO);
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

    private String getProviderId(String providerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(providerToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    kakaoUserInfoUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            // 카카오의 응답값 파싱
            Map<String, Object> body = response.getBody();
            if (body == null || body.get(kakaoUserNameAttribute) == null) {
                throw new InvalidKakaoTokenException();
            }

            return body.get(kakaoUserNameAttribute).toString();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new InvalidKakaoTokenException();
        }
    }

    @Override
    public void revoke(String providerToken) {
        // 모바일 앱의 요청인 경우 프론트에서 처리하므로 무시
        if (providerToken.equals(mobileTokenValue)) {
            return;
        }

        String providerId = getProviderId(providerToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(providerToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target_id_type", targetIdType);
        params.add("target_id", providerId);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        restTemplate.postForEntity(revokeUri, request, String.class);
    }
}
