package com.dongsoop.dongsoop.timetable.notification.query;

import com.dongsoop.dongsoop.timetable.dto.TodayTimetable;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import java.time.DayOfWeek;
import java.time.Year;
import java.util.List;

public interface TimetableNotificationQuery {
    List<TodayTimetable> findTodayTimetables(Year year, SemesterType semester, DayOfWeek week);
}
