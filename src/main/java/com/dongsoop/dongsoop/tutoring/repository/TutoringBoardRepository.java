package com.dongsoop.dongsoop.tutoring.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardOverview;
import com.dongsoop.dongsoop.tutoring.entity.TutoringBoard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TutoringBoardRepository extends JpaRepository<TutoringBoard, Long> {

    @Query("SELECT new com.dongsoop.dongsoop.tutoring.dto.TutoringBoardOverview(b.recruitmentCapacity, b.endAt, b.title, b.content, b.tags) "
            + "FROM TutoringBoard b "
            + "WHERE b.department = :recruitmentDepartment AND "
            + " b.endAt > CURRENT_TIMESTAMP")
    List<TutoringBoardOverview> findAllTutoringBoardOverviews(Department recruitmentDepartment);
}
