package com.dongsoop.dongsoop.calendar.notification.query;

import com.dongsoop.dongsoop.calendar.dto.TodaySchedule;
import java.util.List;

public interface CalendarNotificationQuery {
    List<TodaySchedule> findTodaySchedules();
}
