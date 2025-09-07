package com.dongsoop.dongsoop.calendar.repository;

import com.dongsoop.dongsoop.calendar.dto.HomeSchedule;
import com.dongsoop.dongsoop.calendar.dto.TodaySchedule;
import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface MemberScheduleRepositoryCustom {

    List<MemberSchedule> findMemberScheduleByDuration(Long memberId, LocalDateTime startAt, LocalDateTime endAt);

    List<TodaySchedule> searchTodaySchedule();

    List<HomeSchedule> searchHomeSchedule(Long memberId, LocalDate date);
}
