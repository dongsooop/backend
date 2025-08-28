package com.dongsoop.dongsoop.notification.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberNotificationId {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_details_id", nullable = false, updatable = false)
    private NotificationDetails details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
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

        boolean isDetailsSame = Objects.equals( // detail null 체크
                details != null ? details.getId() : null,
                that.details != null ? that.details.getId() : null
        );

        boolean isMemberSame = Objects.equals( // member null 체크
                member != null ? member.getId() : null,
                that.member != null ? that.member.getId() : null
        );

        return isDetailsSame && isMemberSame;
    }

    @Override
    public int hashCode() {
        return Objects.hash(details, member);
    }
}
