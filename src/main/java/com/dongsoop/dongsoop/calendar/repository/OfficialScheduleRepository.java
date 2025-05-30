package com.dongsoop.dongsoop.calendar.repository;

import com.dongsoop.dongsoop.calendar.entity.OfficialSchedule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialScheduleRepository extends JpaRepository<OfficialSchedule, Long> {

    List<OfficialSchedule> findByStartAtIsGreaterThanEqualAndEndAtIsLessThan(LocalDate startAt,
                                                                             LocalDate endAt);
}
