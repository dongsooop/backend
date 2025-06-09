package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.recruitment.project.entity.ProjectApply;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectApply.ProjectApplyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectApplyRepository extends JpaRepository<ProjectApply, ProjectApplyKey> {
}
