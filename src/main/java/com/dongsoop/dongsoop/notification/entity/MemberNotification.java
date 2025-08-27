package com.dongsoop.dongsoop.notification.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberNotification extends BaseEntity {

    @Id
    @EmbeddedId
    private MemberNotificationId id;

    public MemberNotification(NotificationDetails details, Member member) {
        this.id = new MemberNotificationId(details, member);
    }
}
