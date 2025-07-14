package com.dongsoop.dongsoop.recruitment.board.tutoring.repository;

import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.TutoringBoard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutoringBoardRepository extends JpaRepository<TutoringBoard, Long> {

    boolean existsByIdAndAuthorId(Long id, Long authorId);
}
