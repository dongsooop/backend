package com.dongsoop.dongsoop.calendar.notification;

import com.dongsoop.dongsoop.member.entity.Member;
import java.util.List;

public interface CalendarNotification {

    void sendForAnonymous(int officialCalendarSize, String integratedBody);

    void saveAndSendForMember(Member member, List<String> devices, String title, String body);
}
