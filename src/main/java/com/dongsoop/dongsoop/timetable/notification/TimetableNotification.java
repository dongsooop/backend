package com.dongsoop.dongsoop.timetable.notification;

import com.dongsoop.dongsoop.timetable.dto.TodayTimetable;
import java.util.List;
import java.util.Map;

public interface TimetableNotification {

    void send(Long memberId, List<TodayTimetable> timetables, Map<Long, List<String>> deviceByMember);
}
