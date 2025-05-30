package com.dongsoop.dongsoop.project.repository;

import com.dongsoop.dongsoop.project.entity.ProjectBoard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectBoardRepository extends JpaRepository<ProjectBoard, Long> {
}
