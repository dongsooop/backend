package com.dongsoop.dongsoop.study.repository;

import com.dongsoop.dongsoop.study.entity.StudyBoardDepartment;
import com.dongsoop.dongsoop.study.entity.StudyBoardDepartment.StudyBoardDepartmentId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyBoardDepartmentRepository extends JpaRepository<StudyBoardDepartment, StudyBoardDepartmentId> {
}
