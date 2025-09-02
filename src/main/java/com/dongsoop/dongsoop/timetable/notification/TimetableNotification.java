package com.dongsoop.dongsoop.timetable.notification;

import java.util.List;

public interface TimetableNotification {

    void send(String title, String body, List<String> devices);
}
