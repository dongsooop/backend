package com.dongsoop.dongsoop.oauth.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.exception.AccountNotLinkedException;
import com.dongsoop.dongsoop.oauth.exception.LinkedAccountAlreadyDeletedException;
import com.dongsoop.dongsoop.oauth.repository.MemberSocialAccountRepository;
import com.dongsoop.dongsoop.report.exception.MemberSanctionedException;
import com.dongsoop.dongsoop.report.validator.ReportValidator;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberSocialAccountValidator 테스트")
class MemberSocialAccountValidatorTest {

    private static final String PROVIDER_ID = "test-id";
    private static final Long MEMBER_ID = 1L;
    @Mock
    private MemberSocialAccountRepository repository;
    @Mock
    private ReportValidator reportValidator;
    @Mock
    private Member member;
    @InjectMocks
    private MemberSocialAccountValidatorImpl validator;

    @Test
    @DisplayName("정상 검증 성공")
    void validate_Success() {
        MemberSocialAccountDto dto = new MemberSocialAccountDto(
                member, PROVIDER_ID, OAuthProviderType.GOOGLE,
                List.of(RoleType.USER)
        );

        when(repository.findMemberSocialAccountDTO(PROVIDER_ID, OAuthProviderType.GOOGLE))
                .thenReturn(Optional.of(dto));
        when(member.getId()).thenReturn(MEMBER_ID);
        when(member.isDeleted()).thenReturn(false);

        MemberSocialAccountDto result = validator.validate(PROVIDER_ID, OAuthProviderType.GOOGLE);

        assertThat(result).isNotNull();
        verify(reportValidator).checkMemberAccessById(MEMBER_ID);
    }

    @Test
    @DisplayName("연동되지 않은 계정")
    void validate_NotLinked() {
        when(repository.findMemberSocialAccountDTO(PROVIDER_ID, OAuthProviderType.GOOGLE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(PROVIDER_ID, OAuthProviderType.GOOGLE))
                .isInstanceOf(AccountNotLinkedException.class);
    }

    @Test
    @DisplayName("회원 매칭 안됨")
    void validate_MemberNotMatched() {
        MemberSocialAccountDto dto = new MemberSocialAccountDto(
                null, PROVIDER_ID, OAuthProviderType.GOOGLE,
                List.of(RoleType.USER)
        );

        when(repository.findMemberSocialAccountDTO(PROVIDER_ID, OAuthProviderType.GOOGLE))
                .thenReturn(Optional.of(dto));

        assertThatThrownBy(() -> validator.validate(PROVIDER_ID, OAuthProviderType.GOOGLE))
                .isInstanceOf(AccountNotLinkedException.class);
    }

    @Test
    @DisplayName("탈퇴된 회원")
    void validate_Deleted() {
        MemberSocialAccountDto dto = new MemberSocialAccountDto(
                member, PROVIDER_ID, OAuthProviderType.GOOGLE,
                List.of(RoleType.USER)
        );

        when(repository.findMemberSocialAccountDTO(PROVIDER_ID, OAuthProviderType.GOOGLE))
                .thenReturn(Optional.of(dto));
        when(member.isDeleted()).thenReturn(true);

        assertThatThrownBy(() -> validator.validate(PROVIDER_ID, OAuthProviderType.GOOGLE))
                .isInstanceOf(LinkedAccountAlreadyDeletedException.class);
    }

    @Test
    @DisplayName("신고당한 회원")
    void validate_Reported() {
        MemberSocialAccountDto dto = new MemberSocialAccountDto(
                member, PROVIDER_ID, OAuthProviderType.GOOGLE,
                List.of(RoleType.USER)
        );

        when(repository.findMemberSocialAccountDTO(PROVIDER_ID, OAuthProviderType.GOOGLE))
                .thenReturn(Optional.of(dto));
        when(member.getId()).thenReturn(MEMBER_ID);
        when(member.isDeleted()).thenReturn(false);
        doThrow(new MemberSanctionedException("제재 중"))
                .when(reportValidator).checkMemberAccessById(MEMBER_ID);

        assertThatThrownBy(() -> validator.validate(PROVIDER_ID, OAuthProviderType.GOOGLE))
                .isInstanceOf(MemberSanctionedException.class);
    }
}
