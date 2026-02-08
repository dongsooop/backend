package com.dongsoop.dongsoop.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.oauth.provider.AppleSocialProvider;
import com.dongsoop.dongsoop.oauth.provider.GoogleSocialProvider;
import com.dongsoop.dongsoop.oauth.provider.KakaoSocialProvider;
import com.dongsoop.dongsoop.oauth.provider.OAuth2UserParser;
import com.dongsoop.dongsoop.oauth.provider.OAuth2UserParserImpl;
import com.dongsoop.dongsoop.oauth.provider.SocialProvider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * OAuth2UserParser 단위 테스트
 * <p>
 * 목적: OAuth2UserParser의 프로바이더 파싱 로직 검증
 */
@DisplayName("OAuth2UserParser - 프로바이더 파싱 테스트")
class OAuth2UserParserTest {

    private OAuth2UserParser oAuth2UserParser;
    private GoogleSocialProvider googleSocialProvider;
    private KakaoSocialProvider kakaoSocialProvider;
    private AppleSocialProvider appleSocialProvider;

    @BeforeEach
    void setUp() {
        // Mock providers
        googleSocialProvider = mock(GoogleSocialProvider.class);
        kakaoSocialProvider = mock(KakaoSocialProvider.class);
        appleSocialProvider = mock(AppleSocialProvider.class);

        when(googleSocialProvider.serviceName()).thenReturn("google");
        when(kakaoSocialProvider.serviceName()).thenReturn("kakao");
        when(appleSocialProvider.serviceName()).thenReturn("apple");

        // Create parser with mocked providers
        List<SocialProvider> providers = List.of(
                googleSocialProvider,
                kakaoSocialProvider,
                appleSocialProvider
        );
        oAuth2UserParser = new OAuth2UserParserImpl(providers);
    }

    @Test
    @DisplayName("OAuth2UserParser가 정상적으로 생성되는지 확인")
    void oauth2UserParserCreated() {
        assertThat(oAuth2UserParser).isNotNull();
    }

    @Test
    @DisplayName("Google 프로바이더를 정상적으로 추출")
    void extractGoogleProvider() {
        // when
        SocialProvider provider = oAuth2UserParser.extractProvider("google");

        // then
        assertThat(provider).isNotNull();
        assertThat(provider.serviceName()).isEqualTo("google");
    }

    @Test
    @DisplayName("Kakao 프로바이더를 정상적으로 추출")
    void extractKakaoProvider() {
        // when
        SocialProvider provider = oAuth2UserParser.extractProvider("kakao");

        // then
        assertThat(provider).isNotNull();
        assertThat(provider.serviceName()).isEqualTo("kakao");
    }

    @Test
    @DisplayName("Apple 프로바이더를 정상적으로 추출")
    void extractAppleProvider() {
        // when
        SocialProvider provider = oAuth2UserParser.extractProvider("apple");

        // then
        assertThat(provider).isNotNull();
        assertThat(provider.serviceName()).isEqualTo("apple");
    }
}
