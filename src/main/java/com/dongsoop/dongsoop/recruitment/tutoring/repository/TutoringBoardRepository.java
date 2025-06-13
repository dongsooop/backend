package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringBoard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutoringBoardRepository extends JpaRepository<TutoringBoard, Long> {

    boolean existsByIdAndAuthor(Long id, Member author);
}
