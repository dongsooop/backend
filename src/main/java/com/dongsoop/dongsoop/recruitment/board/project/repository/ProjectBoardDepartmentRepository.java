package com.dongsoop.dongsoop.recruitment.board.project.repository;

import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoardDepartment;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoardDepartment.ProjectBoardDepartmentId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectBoardDepartmentRepository extends
        JpaRepository<ProjectBoardDepartment, ProjectBoardDepartmentId> {

    @Query("SELECT sbd FROM ProjectBoardDepartment sbd WHERE sbd.id.projectBoard.id = :boardId")
    List<ProjectBoardDepartment> findByProjectBoardId(@Param("boardId") Long boardId);
}
