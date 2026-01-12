package com.dongsoop.dongsoop.oauth.repository;

import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccount;
import com.dongsoop.dongsoop.oauth.entity.MemberSocialAccountId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, MemberSocialAccountId>,
        MemberSocialAccountRepositoryCustom {

    List<MemberSocialAccount> findByMemberId(Long memberId);
}
