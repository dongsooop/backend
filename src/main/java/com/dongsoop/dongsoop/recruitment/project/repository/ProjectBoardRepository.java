package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectBoardRepository extends JpaRepository<ProjectBoard, Long> {

    boolean existsByIdAndAuthorId(Long id, Long authorId);
}
