package com.dongsoop.dongsoop.recruitment.board.study.repository;

import com.dongsoop.dongsoop.recruitment.board.study.entity.StudyBoard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyBoardRepository extends JpaRepository<StudyBoard, Long> {

    boolean existsByIdAndAuthorId(Long id, Long authorId);
}
