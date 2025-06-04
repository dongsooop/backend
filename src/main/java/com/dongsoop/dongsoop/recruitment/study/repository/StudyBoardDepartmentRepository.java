package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoardDepartment;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoardDepartment.StudyBoardDepartmentId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyBoardDepartmentRepository extends JpaRepository<StudyBoardDepartment, StudyBoardDepartmentId> {
}
