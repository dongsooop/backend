package com.dongsoop.dongsoop.oauth.validator;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccountId;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.exception.AccountNotLinkedException;
import com.dongsoop.dongsoop.oauth.exception.AlreadyLinkedProviderTypeException;
import com.dongsoop.dongsoop.oauth.exception.AlreadyLinkedSocialAccountException;
import com.dongsoop.dongsoop.oauth.exception.LinkedAccountAlreadyDeletedException;
import com.dongsoop.dongsoop.oauth.repository.MemberSocialAccountRepository;
import com.dongsoop.dongsoop.report.validator.ReportValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberSocialAccountValidatorImpl implements MemberSocialAccountValidator {

    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final ReportValidator reportValidator;

    @Override
    public MemberSocialAccountDto validate(String providerId, OAuthProviderType providerType) {
        // 소셜 계정과 연동된 회원이 있는지 확인
        MemberSocialAccountDto socialAccount = memberSocialAccountRepository.findMemberSocialAccountDTO(providerId,
                        providerType)
                .orElseThrow(() -> {
                    log.info("social account not linked: providerId={}", providerId);
                    return new AccountNotLinkedException();
                });

        // 소셜 계정이 회원과 매칭되지 않은 경우 예외 처리
        if (socialAccount.isMemberNotMatched()) {
            log.info("social account not linked: providerId={}", providerId);
            throw new AccountNotLinkedException();
        }

        // 소셜 계정과 연결된 계정이 탈퇴된 경우 예외 처리
        if (socialAccount.isMemberDeleted()) {
            log.info("linked account was deleted: providerId={}", providerId);
            throw new LinkedAccountAlreadyDeletedException();
        }

        // 신고당한 회원인지 검증
        reportValidator.checkMemberAccessById(socialAccount.member().getId());

        return socialAccount;
    }

    @Override
    public void validateAlreadyLinked(MemberSocialAccountId socialAccountId, Member member,
                                      OAuthProviderType providerType) {
        // 이미 DB에 저장된 소셜 계정인 경우 예외 처리
        if (this.memberSocialAccountRepository.existsById(socialAccountId)) {
            throw new AlreadyLinkedSocialAccountException();
        }

        // 이미 회원이 해당 프로바이더 타입으로 가입한 경우 예외 처리
        this.memberSocialAccountRepository.findByMemberAndProviderType(member, providerType)
                .ifPresent((account) -> {
                    throw new AlreadyLinkedProviderTypeException();
                });
    }
}
