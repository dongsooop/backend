package com.dongsoop.dongsoop.study.repository;

import com.dongsoop.dongsoop.study.entity.StudyBoard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyBoardRepository extends JpaRepository<StudyBoard, Long> {
}
