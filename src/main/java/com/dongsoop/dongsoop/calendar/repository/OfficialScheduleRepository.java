package com.dongsoop.dongsoop.calendar.repository;

import com.dongsoop.dongsoop.calendar.entity.OfficialSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialScheduleRepository extends JpaRepository<OfficialSchedule, Long>,
        OfficialScheduleRepositoryCustom {
}
