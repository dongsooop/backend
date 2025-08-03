package com.dongsoop.dongsoop.memberblock.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberBlockId {

    @JoinColumn(name = "blocker_id")
    @ManyToOne
    private Member blocker;

    @JoinColumn(name = "blocked_member_id")
    @ManyToOne
    private Member blockedMember;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MemberBlockId that = (MemberBlockId) o;
        return Objects.equals(blocker, that.blocker) && Objects.equals(blockedMember, that.blockedMember);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blocker.getId(), blockedMember.getId());
    }
}
