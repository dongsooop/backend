package com.dongsoop.dongsoop.member.validate;

import com.dongsoop.dongsoop.exception.domain.member.EmailDuplicatedException;
import com.dongsoop.dongsoop.exception.domain.member.NicknameDuplicatedException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberDuplicationValidatorImpl implements MemberDuplicationValidator {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public void validateNicknameDuplication(String nickname) {
        boolean isExists = memberRepository.existsByNickname(nickname);

        if (isExists) {
            throw new NicknameDuplicatedException();
        }
    }

    @Transactional(readOnly = true)
    public void validateEmailDuplication(String email) {
        boolean isExists = memberRepository.existsByEmail(email);

        if (isExists) {
            throw new EmailDuplicatedException();
        }
    }
}
