package com.dongsoop.dongsoop.notification.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@SequenceGenerator(name = "notification_details_sequence_generator")
public class NotificationDetails extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_details_sequence_generator")
    private Long id;

    private String title;

    private String body;

    private NotificationType type;

    private String value;

    @Builder.Default
    private boolean isRead = false;
}
