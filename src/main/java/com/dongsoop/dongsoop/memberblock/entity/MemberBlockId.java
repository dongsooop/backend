package com.dongsoop.dongsoop.memberblock.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class MemberBlockId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long blocker;

    private Long blockedMember;

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
        return Objects.hash(blocker, blockedMember);
    }
}
