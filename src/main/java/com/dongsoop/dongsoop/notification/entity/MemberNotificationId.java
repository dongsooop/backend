package com.dongsoop.dongsoop.notification.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberNotificationId {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_details_id")
    private NotificationDetails details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MemberNotificationId that = (MemberNotificationId) o;

        return that.details.getId().equals(this.details.getId()) && that.member.getId().equals(this.member.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(details, member);
    }
}
