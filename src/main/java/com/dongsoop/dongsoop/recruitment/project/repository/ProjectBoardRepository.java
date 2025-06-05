package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoard;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoardDepartment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectBoardRepository extends JpaRepository<ProjectBoard, Long> {

    @Query("SELECT pbd FROM ProjectBoardDepartment pbd WHERE pbd.id.projectBoard.id = :boardId")
    List<ProjectBoardDepartment> findByProjectBoardId(@Param("boardId") Long boardId);
}
