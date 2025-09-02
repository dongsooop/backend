package com.dongsoop.dongsoop.calendar.repository;

import com.dongsoop.dongsoop.calendar.entity.OfficialSchedule;
import java.time.LocalDate;
import java.util.List;

public interface OfficialScheduleRepositoryCustom {

    List<OfficialSchedule> findOfficialScheduleByDuration(LocalDate startAt, LocalDate endAt);

    List<String> searchTodaySchedule();
}
