package com.dongsoop.dongsoop.role.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@IdClass(MemberRole.MemberRoleKey.class)
@NoArgsConstructor
@AllArgsConstructor
public class MemberRole {

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberRoleKey implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long member;

        private Long role;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MemberRoleKey that = (MemberRoleKey) o;
            return Objects.equals(member, that.member)
                    && Objects.equals(role, that.role);
        }

        @Override
        public int hashCode() {
            return Objects.hash(member, role);
        }
    }
}
