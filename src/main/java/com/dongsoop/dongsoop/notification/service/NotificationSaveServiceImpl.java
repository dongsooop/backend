package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import com.dongsoop.dongsoop.notification.repository.NotificationDetailsRepository;
import com.dongsoop.dongsoop.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSaveServiceImpl implements NotificationSaveService {

    private final NotificationDetailsRepository notificationDetailsRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public List<MemberNotification> saveAll(List<Member> memberList, String title, String body,
                                            NotificationType type,
                                            String value) {
        NotificationDetails details = NotificationDetails.builder()
                .title(title)
                .body(body)
                .type(type)
                .value(value)
                .build();

        notificationDetailsRepository.save(details);

        List<MemberNotification> memberNotificationList = memberList.stream()
                .map(member -> new MemberNotification(details, member))
                .toList();

        return notificationRepository.saveAll(memberNotificationList);
    }

    @Override
    @Transactional
    public MemberNotification save(Member member, String title, String body,
                                   NotificationType type,
                                   String value) {
        NotificationDetails details = NotificationDetails.builder()
                .title(title)
                .body(body)
                .type(type)
                .value(value)
                .build();

        notificationDetailsRepository.save(details);

        MemberNotification memberNotification = new MemberNotification(details, member);
        return notificationRepository.save(memberNotification);
    }
}
