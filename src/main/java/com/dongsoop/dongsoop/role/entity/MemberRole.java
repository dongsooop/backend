package com.dongsoop.dongsoop.role.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class MemberRole {

    @EmbeddedId
    @Column(unique = true, nullable = false)
    private MemberRoleKey id;

    public MemberRole(Member member, Role role) {
        this.id = new MemberRoleKey(member, role);
    }

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberRoleKey {

        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "member_id")
        private Member member;

        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "role_id")
        private Role role;
    }
}
