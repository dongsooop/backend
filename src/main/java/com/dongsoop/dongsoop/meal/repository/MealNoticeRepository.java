package com.dongsoop.dongsoop.meal.repository;

import com.dongsoop.dongsoop.meal.entity.MealNotice;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MealNoticeRepository extends JpaRepository<MealNotice, Long> {

    Optional<MealNotice> findByWeekStart(LocalDate weekStart);

    void deleteByWeekStart(LocalDate weekStart);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MealNotice n WHERE n.weekStart < :cutoffDate")
    int deleteOldNotices(@Param("cutoffDate") LocalDate cutoffDate);
}
