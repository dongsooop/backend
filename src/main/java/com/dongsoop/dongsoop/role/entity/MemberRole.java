package com.dongsoop.dongsoop.role.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
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
        @JoinColumn(name = "member_id", nullable = false)
        private Member member;

        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn(name = "role_id", nullable = false)
        private Role role;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MemberRoleKey that = (MemberRoleKey) o;
            return Objects.equals(member.getId(), that.member.getId())
                    && Objects.equals(role.getRoleType(), that.role.getRoleType());
        }

        @Override
        public int hashCode() {
            return Objects.hash(member.getId(), role.getRoleType());
        }
    }
}
