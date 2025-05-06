package com.dongsoop.dongsoop.tutoring.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardDetails;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardOverview;
import com.dongsoop.dongsoop.tutoring.entity.TutoringBoard;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TutoringBoardRepository extends JpaRepository<TutoringBoard, Long> {

    @Query("SELECT new com.dongsoop.dongsoop.tutoring.dto.TutoringBoardOverview(b.recruitmentCapacity, b.endAt, b.title, b.content, b.tags) "
            + "FROM TutoringBoard b "
            + "WHERE b.department = :recruitmentDepartment AND "
            + " b.endAt > CURRENT_TIMESTAMP")
    Page<TutoringBoardOverview> findTutoringBoardOverviewsByPage(
            @Param("recruitmentDepartment") Department recruitmentDepartment, Pageable Pageable);

    @Query("SELECT "
            + "b.id as id, "
            + "b.title as title, "
            + "b.content as content, "
            + "b.startAt as startAt, "
            + "b.endAt as endAt, "
            + "b.recruitmentCapacity as recruitmentCapacity, "
            + "b.tags as tags, "
            + "b.author.nickname as author, "
            + "b.department.id as departmentType, "
            + "b.boardDate.createdAt as createdAt, "
            + "b.boardDate.updatedAt as updatedAt "
            + "FROM TutoringBoard b "
            + "WHERE b.id = :tutoringBoardId")
    Optional<TutoringBoardDetails> findInformationById(@Param("tutoringBoardId") Long tutoringBoardId);
}
