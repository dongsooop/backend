package com.dongsoop.dongsoop.timetable.repository;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.timetable.dto.TimetableView;
import com.dongsoop.dongsoop.timetable.dto.YearSemester;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.dongsoop.dongsoop.timetable.entity.Timetable;
import java.time.Year;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableRepository extends JpaRepository<Timetable, Long>, TimetableRepositoryCustom {
    
    List<TimetableView> findAllByMemberAndYearAndSemester(Member member, Year year, SemesterType semester);

    YearSemester findYearSemesterById(Long id);
}
