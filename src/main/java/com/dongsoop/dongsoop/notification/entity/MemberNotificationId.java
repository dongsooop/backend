package com.dongsoop.dongsoop.notification.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberNotificationId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long details;

    private Long member;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MemberNotificationId that = (MemberNotificationId) o;

        return Objects.equals(details, that.details) && Objects.equals(member, that.member);
    }

    @Override
    public int hashCode() {
        return Objects.hash(details, member);
    }
}
