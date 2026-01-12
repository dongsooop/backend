package com.dongsoop.dongsoop.oauth.validator;

import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;

public interface MemberSocialAccountValidator {

    MemberSocialAccountDto validate(String providerId);
}
