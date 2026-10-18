package com.dongsoop.dongsoop.oauth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.exception.TokenExpiredException;
import com.dongsoop.dongsoop.jwt.exception.TokenSignatureException;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.oauth.dto.AppleJwk;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.dto.SocialAccountLinkRequest;
import com.dongsoop.dongsoop.oauth.entity.*;
import com.dongsoop.dongsoop.oauth.exception.*;
import com.dongsoop.dongsoop.oauth.provider.*;
import com.dongsoop.dongsoop.oauth.repository.MemberSocialAccountRepository;
import com.dongsoop.dongsoop.oauth.validator.MemberSocialAccountValidator;
import com.dongsoop.dongsoop.role.entity.Role;
import com.dongsoop.dongsoop.role.entity.RoleType;
import com.dongsoop.dongsoop.role.repository.MemberRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AppleIdentityContractTest {
    private static final KeyPair KEYS = Jwts.SIG.RS256.keyPair().build();
    private static final String ISSUER = "https://apple.example.com";
    @Mock MemberSocialAccountValidator validator;
    @Mock MemberRepository members;
    @Mock MemberRoleRepository roles;
    @Mock MemberSocialAccountRepository accounts;
    @Mock AppleJwkProvider jwks;
    private final RestTemplate rest = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
    private AppleSocialProvider provider;
    private final Member member = Member.builder().id(7L).build();

    @BeforeEach
    void setup() {
        provider = new AppleSocialProvider(validator, members, roles, accounts, new ObjectMapper(), jwks, rest, new JwtUtil(null));
        ReflectionTestUtils.setField(provider, "issuer", ISSUER);
        ReflectionTestUtils.setField(provider, "appleClientId", "test-client");
        ReflectionTestUtils.setField(provider, "appleUserNameAttribute", "sub");
        ReflectionTestUtils.setField(provider, "revokeUri", "https://example.com/revoke");
        ReflectionTestUtils.setField(provider, "clientId", "test-client");
        ReflectionTestUtils.setField(provider, "clientSecret", "fake-secret");
    }

    @Test
    void signedIdentityAuthenticatesMemberAndRolesAfterKeyRotation() {
        when(jwks.getAppleJwkMap()).thenReturn(Map.of()).thenReturn(keyMap());
        when(validator.validate("apple-7", OAuthProviderType.APPLE))
                .thenReturn(new MemberSocialAccountDto(member, "apple-7", OAuthProviderType.APPLE, List.of(RoleType.USER)));
        Role role = mock(Role.class);
        when(role.getRoleType()).thenReturn(RoleType.USER);
        when(roles.findAllByMemberId(7L)).thenReturn(List.of(role));
        var authentication = provider.login(token(ISSUER, "test-client", KEYS, Instant.now().plusSeconds(600)));
        assertThat(authentication.getName()).isEqualTo("7");
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("USER");
        verify(jwks).evictAppleJwkCache();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "a.b.c", "e30.b.c", "eyJraWQiOiJ1bmtub3duIn0.b.c"})
    void invalidHeaderOrUnknownKeyCannotReachMemberLookup(String token) {
        lenient().when(jwks.getAppleJwkMap()).thenReturn(keyMap());
        assertThatThrownBy(() -> provider.login(token)).isInstanceOf(InvalidAppleTokenException.class);
        verifyNoInteractions(validator, roles, accounts);
    }

    @Test
    void cryptographicSignatureAndExpirationErrorsKeepTokenTypes() {
        when(jwks.getAppleJwkMap()).thenReturn(keyMap());
        String expired = token(ISSUER, "test-client", KEYS, Instant.now().minusSeconds(600));
        String forged = token(ISSUER, "test-client", Jwts.SIG.RS256.keyPair().build(), Instant.now().plusSeconds(600));
        assertThatThrownBy(() -> provider.login(expired)).isInstanceOf(TokenExpiredException.class);
        assertThatThrownBy(() -> provider.login(forged)).isInstanceOf(TokenSignatureException.class);
        verifyNoInteractions(validator);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void wrongIssuerOrAudienceCannotAuthenticate(boolean wrongIssuer) {
        when(jwks.getAppleJwkMap()).thenReturn(keyMap());
        String issuer = ISSUER;
        String audience = "wrong-client";
        if (wrongIssuer) { issuer = "https://wrong.example.com"; audience = "test-client"; }
        String identity = token(issuer, audience, KEYS, Instant.now().plusSeconds(600));
        assertThatThrownBy(() -> provider.login(identity)).isInstanceOf(InvalidAppleTokenException.class);
        verifyNoInteractions(validator);
    }

    @Test
    void linkStoresProviderIdentityAndMemberAndReturnsSavedTime() {
        when(jwks.getAppleJwkMap()).thenReturn(keyMap());
        when(members.findAndLockById(7L)).thenReturn(member);
        when(accounts.save(any())).thenAnswer(call -> call.getArgument(0));
        String identity = token(ISSUER, "test-client", KEYS, Instant.now().plusSeconds(600));
        var created = provider.linkSocialAccount(7L, new SocialAccountLinkRequest(identity));
        ArgumentCaptor<MemberSocialAccount> stored = ArgumentCaptor.forClass(MemberSocialAccount.class);
        verify(accounts).save(stored.capture());
        assertThat(stored.getValue().getMember()).isSameAs(member);
        assertThat(stored.getValue()).extracting("id").isEqualTo(new MemberSocialAccountId("apple-7", OAuthProviderType.APPLE));
        assertThat(created).isEqualTo(stored.getValue().getCreatedAt());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void duplicateIdentityAndProviderHaveDistinctErrors(boolean existingIdentity) {
        when(jwks.getAppleJwkMap()).thenReturn(keyMap());
        when(accounts.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(accounts.existsById(any())).thenReturn(existingIdentity);
        String identity = token(ISSUER, "test-client", KEYS, Instant.now().plusSeconds(600));
        Class<? extends RuntimeException> expected = AlreadyLinkedProviderTypeException.class;
        if (existingIdentity) { expected = AlreadyLinkedSocialAccountException.class; }
        assertThatThrownBy(() -> provider.linkSocialAccount(7L, new SocialAccountLinkRequest(identity))).isInstanceOf(expected);
    }

    @Test
    void oauthUserRequiresConfiguredSubjectAttribute() {
        OAuth2User user = mock(OAuth2User.class);
        when(user.getAttributes()).thenReturn(Map.of("sub", "apple-7")).thenReturn(Map.of());
        assertThat(provider.extractProviderId(user, "apple")).isEqualTo("apple-7");
        assertThatThrownBy(() -> provider.extractProviderId(user, "apple")).isInstanceOf(InvalidAppleTokenException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 400, 500})
    void revokeSendsFormAndTranslatesRemoteErrors(int status) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("client_id", "test-client"); form.add("client_secret", "fake-secret");
        form.add("token", "fake-refresh"); form.add("token_type_hint", "refresh_token");
        server.expect(requestTo("https://example.com/revoke")).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(form)).andRespond(withStatus(HttpStatus.valueOf(status)));
        if (status == 200) { assertThatCode(() -> provider.revoke("fake-refresh")).doesNotThrowAnyException(); }
        if (status != 200) { assertThatThrownBy(() -> provider.revoke("fake-refresh")).isInstanceOf(InvalidAppleTokenException.class); }
        server.verify();
    }

    private Map<String, AppleJwk> keyMap() {
        RSAPublicKey key = (RSAPublicKey) KEYS.getPublic();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return Map.of("key-1", new AppleJwk("RSA", "key-1", "sig", "RS256",
                encoder.encodeToString(key.getModulus().toByteArray()), encoder.encodeToString(key.getPublicExponent().toByteArray())));
    }

    // JJWT signing API requires Date at this boundary.
    @SuppressWarnings("java:S2143")
    private String token(String issuer, String audience, KeyPair pair, Instant expiration) {
        return Jwts.builder().header().keyId("key-1").and().subject("apple-7").issuer(issuer)
                .audience().add(audience).and().expiration(java.util.Date.from(expiration)).signWith(pair.getPrivate()).compact();
    }
}
