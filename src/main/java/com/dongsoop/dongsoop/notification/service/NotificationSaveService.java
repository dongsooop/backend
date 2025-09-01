package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import java.util.List;

public interface NotificationSaveService {

    List<MemberNotification> saveAll(List<Member> memberList, String title, String body,
                                     NotificationType type,
                                     String value);

    MemberNotification save(Member member, String title, String body,
                            NotificationType type,
                            String value);
}
