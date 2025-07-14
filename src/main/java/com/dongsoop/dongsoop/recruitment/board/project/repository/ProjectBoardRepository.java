package com.dongsoop.dongsoop.recruitment.board.project.repository;

import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectBoardRepository extends JpaRepository<ProjectBoard, Long> {

    boolean existsByIdAndAuthorId(Long id, Long authorId);
}
