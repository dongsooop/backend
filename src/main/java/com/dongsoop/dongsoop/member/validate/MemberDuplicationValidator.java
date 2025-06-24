package com.dongsoop.dongsoop.member.validate;

public interface MemberDuplicationValidator {

    void validateNicknameDuplication(String nickname);

    void validateEmailDuplication(String email);
}
