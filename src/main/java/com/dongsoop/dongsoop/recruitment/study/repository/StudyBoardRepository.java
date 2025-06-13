package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyBoardRepository extends JpaRepository<StudyBoard, Long> {

    boolean existsByIdAndAuthorId(Long id, Long authorId);
}
