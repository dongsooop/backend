package com.dongsoop.dongsoop.oauth.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class MemberSocialAccount {

    @EmbeddedId
    private MemberSocialAccountId id;

    @Getter
    @JoinColumn(name = "member_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    public void updateMember(Member member) {
        this.member = member;
    }
}
