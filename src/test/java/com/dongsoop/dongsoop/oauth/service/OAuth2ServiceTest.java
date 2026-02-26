package com.dongsoop.dongsoop.oauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.member.dto.LoginMemberDetails;
import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccount;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.exception.InvalidProviderTypeException;
import com.dongsoop.dongsoop.oauth.provider.GoogleSocialProvider;
import com.dongsoop.dongsoop.oauth.provider.OAuth2UserParser;
import com.dongsoop.dongsoop.oauth.repository.MemberSocialAccountRepository;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

/**
 * OAuth2ServiceImpl 단위 테스트
 * <p>
 * 목적: OAuth2Service의 핵심 비즈니스 로직 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2Service - 소셜 계정 관리 로직 테스트")
class OAuth2ServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    @Mock
    private MemberSocialAccountRepository memberSocialAccountRepository;

    @Mock
    private OAuth2UserParser oAuth2UserParser;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberDeviceRepository memberDeviceRepository;

    @Mock
    private TokenGenerator tokenGenerator;

    @Mock
    private GoogleSocialProvider googleSocialProvider;

    @Mock
    private MemberSocialAccount memberSocialAccount;

    @InjectMocks
    private OAuth2ServiceImpl oAuth2Service;

    @Test
    @DisplayName("로그인 처리 성공 - Access/Refresh Token 생성")
    void acceptLogin_Success() {
        // given
        Authentication authentication = mock(Authentication.class);
        LoginMemberDetails memberDetails = new LoginMemberDetails(
                MEMBER_ID,
                "테스터",
                "test@dongyang.ac.kr",
                DepartmentType.DEPT_2001,
                List.of(RoleType.USER)
        );

        when(tokenGenerator.generateAccessToken(eq(authentication), any())).thenReturn(ACCESS_TOKEN);
        when(tokenGenerator.generateRefreshToken(eq(authentication), any())).thenReturn(REFRESH_TOKEN);
        when(memberRepository.findLoginMemberDetailById(MEMBER_ID)).thenReturn(Optional.of(memberDetails));

        // when
        LoginResponse response = oAuth2Service.acceptLogin(authentication, MEMBER_ID, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(response.getId()).isEqualTo(MEMBER_ID);
        assertThat(response.getNickname()).isEqualTo("테스터");

        verify(tokenGenerator).generateAccessToken(eq(authentication), any());
        verify(tokenGenerator).generateRefreshToken(eq(authentication), any());
        verify(memberRepository).findLoginMemberDetailById(MEMBER_ID);
    }

    @Test
    @DisplayName("로그인 처리 실패 - 회원을 찾을 수 없음")
    void acceptLogin_MemberNotFound() {
        // given
        Authentication authentication = mock(Authentication.class);
        when(tokenGenerator.generateAccessToken(eq(authentication), any())).thenReturn(ACCESS_TOKEN);
        when(tokenGenerator.generateRefreshToken(eq(authentication), any())).thenReturn(REFRESH_TOKEN);
        when(memberRepository.findLoginMemberDetailById(MEMBER_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> oAuth2Service.acceptLogin(authentication, MEMBER_ID, null))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("회원의 모든 소셜 계정 연동 해제 - 성공")
    void unlinkMember_Success() {
        // given
        List<MemberSocialAccount> accounts = List.of(memberSocialAccount);
        when(memberSocialAccountRepository.findByMemberId(MEMBER_ID)).thenReturn(accounts);

        // when
        oAuth2Service.unlinkMember(MEMBER_ID);

        // then
        verify(memberSocialAccountRepository).deleteAll(accounts);
    }

    @Test
    @DisplayName("프로바이더 타입별 계정 연동 해제 - 성공")
    void unlinkMemberWithProviderType_Success() {
        // given
        when(memberSocialAccountRepository.findByMemberIdAndProviderType(MEMBER_ID, OAuthProviderType.GOOGLE))
                .thenReturn(Optional.of(memberSocialAccount));
        when(oAuth2UserParser.extractProvider("google")).thenReturn(googleSocialProvider);

        // when
        oAuth2Service.unlinkMemberWithProviderType(
                MEMBER_ID,
                OAuthProviderType.GOOGLE,
                new com.dongsoop.dongsoop.oauth.dto.UnlinkSocialAccountRequest("token")
        );

        // then
        verify(memberSocialAccountRepository).delete(memberSocialAccount);
        verify(googleSocialProvider).revoke("token");
    }

    @Test
    @DisplayName("프로바이더 타입별 계정 연동 해제 - 잘못된 프로바이더")
    void unlinkMemberWithProviderType_InvalidProvider() {
        // given
        when(memberSocialAccountRepository.findByMemberIdAndProviderType(MEMBER_ID, OAuthProviderType.GOOGLE))
                .thenReturn(Optional.empty());
        when(oAuth2UserParser.extractProvider("google")).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> oAuth2Service.unlinkMemberWithProviderType(
                MEMBER_ID,
                OAuthProviderType.GOOGLE,
                new com.dongsoop.dongsoop.oauth.dto.UnlinkSocialAccountRequest("token")
        )).isInstanceOf(InvalidProviderTypeException.class);
    }

    @Test
    @DisplayName("소셜 계정 연동 상태 조회 - 성공")
    void getSocialAccountState_Success() {
        // given
        var overview = List.of(
                new com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountOverview(
                        OAuthProviderType.GOOGLE,
                        java.time.LocalDateTime.now()
                )
        );
        when(memberSocialAccountRepository.findAllMemberSocialAccountOverview(MEMBER_ID))
                .thenReturn(overview);

        // when
        var result = oAuth2Service.getSocialAccountState(MEMBER_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).providerType()).isEqualTo(OAuthProviderType.GOOGLE);
    }
}
