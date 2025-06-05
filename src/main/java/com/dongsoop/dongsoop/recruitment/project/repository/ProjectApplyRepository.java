package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoardApply;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoardApply.ProjectBoardApplyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectApplyRepository extends JpaRepository<ProjectBoardApply, ProjectBoardApplyKey> {
}
