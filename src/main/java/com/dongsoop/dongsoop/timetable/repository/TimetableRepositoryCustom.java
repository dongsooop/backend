package com.dongsoop.dongsoop.timetable.repository;

import com.dongsoop.dongsoop.timetable.dto.OverlapTimetable;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Year;
import java.util.Optional;

public interface TimetableRepositoryCustom {

    Optional<OverlapTimetable> findOverlapWithinRange(Long memberId, Year year, SemesterType semester, DayOfWeek week,
                                                      LocalTime startAt, LocalTime endAt);

    boolean existsByIdAndMemberId(Long id, Long memberId);

    Optional<OverlapTimetable> findOverlapWithinRangeExcludingSelf(Long timetableId, Long memberId, Year year,
                                                                   SemesterType semester,
                                                                   DayOfWeek week,
                                                                   LocalTime startAt, LocalTime endAt);

    void deleteByMemberIdAndYearAndSemester(Long memberId, Year year, SemesterType semester);
}
