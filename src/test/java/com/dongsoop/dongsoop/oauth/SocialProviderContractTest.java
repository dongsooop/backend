package com.dongsoop.dongsoop.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.dto.SocialAccountLinkRequest;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccount;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.exception.InvalidGoogleTokenException;
import com.dongsoop.dongsoop.oauth.exception.InvalidKakaoTokenException;
import com.dongsoop.dongsoop.oauth.exception.AlreadyLinkedProviderTypeException;
import com.dongsoop.dongsoop.oauth.exception.AlreadyLinkedSocialAccountException;
import com.dongsoop.dongsoop.oauth.provider.GoogleSocialProvider;
import com.dongsoop.dongsoop.oauth.provider.KakaoSocialProvider;
import com.dongsoop.dongsoop.oauth.provider.SocialProvider;
import com.dongsoop.dongsoop.oauth.repository.MemberSocialAccountRepository;
import com.dongsoop.dongsoop.oauth.validator.MemberSocialAccountValidator;
import com.dongsoop.dongsoop.role.entity.Role;
import com.dongsoop.dongsoop.role.entity.RoleType;
import com.dongsoop.dongsoop.role.repository.MemberRoleRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class SocialProviderContractTest {
    @Mock MemberSocialAccountValidator validator;
    @Mock MemberSocialAccountRepository accounts;
    @Mock MemberRepository members;
    @Mock MemberRoleRepository roles;
    private final RestTemplate rest = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();

    @AfterEach
    void verifyHttpContract() {
        server.verify();
    }

    @ParameterizedTest
    @EnumSource(value = OAuthProviderType.class, names = {"GOOGLE", "KAKAO"})
    void validProviderResponseProducesMemberAuthentication(OAuthProviderType type) {
        SocialProvider provider = provider(type);
        expectProfile(type);
        Member member = Member.builder().id(7L).build();
        when(validator.validate("provider-7", type))
                .thenReturn(new MemberSocialAccountDto(member, "provider-7", type, List.of(RoleType.USER)));
        Role role = mock(Role.class);
        when(role.getRoleType()).thenReturn(RoleType.USER);
        when(roles.findAllByMemberId(7L)).thenReturn(List.of(role));
        Authentication authentication = provider.login("fake-token");
        assertThat(authentication.getName()).isEqualTo("7");
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("USER");
    }

    @ParameterizedTest
    @EnumSource(value = OAuthProviderType.class, names = {"GOOGLE", "KAKAO"})
    void missingProviderIdIsRejectedBeforeAccountLookup(OAuthProviderType type) {
        SocialProvider provider = provider(type);
        server.expect(requestTo("https://example.com/profile")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> provider.login("fake-token")).isInstanceOf(invalidTokenType(type));
        verifyNoInteractions(validator, accounts, roles);
    }

    @ParameterizedTest
    @EnumSource(value = OAuthProviderType.class, names = {"GOOGLE", "KAKAO"})
    void remoteUnauthorizedIsRejectedBeforeAccountLookup(OAuthProviderType type) {
        SocialProvider provider = provider(type);
        server.expect(requestTo("https://example.com/profile")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> provider.login("fake-token")).isInstanceOf(invalidTokenType(type));
        verifyNoInteractions(validator, accounts, roles);
    }

    @ParameterizedTest
    @EnumSource(value = OAuthProviderType.class, names = {"GOOGLE", "KAKAO"})
    void linkingPersistsMemberAndReturnsCreationTime(OAuthProviderType type) {
        SocialProvider provider = provider(type);
        expectProfile(type);
        Member member = Member.builder().id(7L).build();
        when(members.findAndLockById(7L)).thenReturn(member);
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 14, 9, 0);
        MemberSocialAccount saved = mock(MemberSocialAccount.class);
        when(saved.getCreatedAt()).thenReturn(createdAt);
        when(accounts.save(any())).thenReturn(saved);
        SocialAccountLinkRequest request = new SocialAccountLinkRequest("fake-token");
        assertThat(provider.linkSocialAccount(7L, request)).isEqualTo(createdAt);
        ArgumentCaptor<MemberSocialAccount> stored = ArgumentCaptor.forClass(MemberSocialAccount.class);
        verify(accounts).save(stored.capture());
        assertThat(stored.getValue().getMember()).isSameAs(member);
    }

    @ParameterizedTest
    @EnumSource(value = OAuthProviderType.class, names = {"GOOGLE", "KAKAO"})
    void duplicateSocialAccountGetsSpecificError(OAuthProviderType type) {
        SocialProvider provider = provider(type);
        expectProfile(type);
        when(accounts.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(accounts.existsById(any())).thenReturn(true);
        SocialAccountLinkRequest request = new SocialAccountLinkRequest("fake-token");
        assertThatThrownBy(() -> provider.linkSocialAccount(7L, request))
                .isInstanceOf(AlreadyLinkedSocialAccountException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OAuthProviderType.class, names = {"GOOGLE", "KAKAO"})
    void duplicateProviderGetsSpecificError(OAuthProviderType type) {
        SocialProvider provider = provider(type);
        expectProfile(type);
        when(accounts.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        SocialAccountLinkRequest request = new SocialAccountLinkRequest("fake-token");
        assertThatThrownBy(() -> provider.linkSocialAccount(7L, request))
                .isInstanceOf(AlreadyLinkedProviderTypeException.class);
    }

    private Class<? extends RuntimeException> invalidTokenType(OAuthProviderType type) {
        if (type == OAuthProviderType.GOOGLE) {
            return InvalidGoogleTokenException.class;
        }
        return InvalidKakaoTokenException.class;
    }

    private void expectProfile(OAuthProviderType type) {
        String attribute = attribute(type);
        server.expect(requestTo("https://example.com/profile")).andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer fake-token"))
                .andRespond(withSuccess("{\"" + attribute + "\":\"provider-7\"}", MediaType.APPLICATION_JSON));
    }

    private String attribute(OAuthProviderType type) {
        if (type == OAuthProviderType.GOOGLE) {
            return "sub";
        }
        return "id";
    }

    private SocialProvider provider(OAuthProviderType type) {
        String prefix = type.name().toLowerCase(java.util.Locale.ROOT);
        SocialProvider provider = new KakaoSocialProvider(validator, accounts, members, roles, rest);
        if (type == OAuthProviderType.GOOGLE) {
            provider = new GoogleSocialProvider(validator, accounts, members, roles, rest);
        }
        ReflectionTestUtils.setField(provider, prefix + "UserInfoUrl", "https://example.com/profile");
        ReflectionTestUtils.setField(provider, prefix + "UserNameAttribute", attribute(type));
        return provider;
    }
}
