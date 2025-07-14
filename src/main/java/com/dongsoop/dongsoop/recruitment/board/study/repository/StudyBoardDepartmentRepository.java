package com.dongsoop.dongsoop.recruitment.board.study.repository;

import com.dongsoop.dongsoop.recruitment.board.study.entity.StudyBoardDepartment;
import com.dongsoop.dongsoop.recruitment.board.study.entity.StudyBoardDepartment.StudyBoardDepartmentId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyBoardDepartmentRepository extends JpaRepository<StudyBoardDepartment, StudyBoardDepartmentId> {

    @Query("SELECT sbd FROM StudyBoardDepartment sbd WHERE sbd.id.studyBoard.id = :boardId")
    List<StudyBoardDepartment> findByStudyBoardId(@Param("boardId") Long boardId);
}
