package com.dongsoop.dongsoop.notification.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberNotification extends BaseEntity {

    @EmbeddedId
    private MemberNotificationId id;

    private boolean isRead = false;

    public MemberNotification(NotificationDetails details, Member member) {
        this.id = new MemberNotificationId(details, member);
    }

    public void delete() {
        super.isDeleted = true;
    }

    public void read() {
        this.isRead = true;
    }
}
