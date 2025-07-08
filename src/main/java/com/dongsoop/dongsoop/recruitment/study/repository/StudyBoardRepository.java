package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StudyBoardRepository extends JpaRepository<StudyBoard, Long> {

    boolean existsByIdAndAuthorId(Long id, Long authorId);

    @Query("""
                    SELECT CASE WHEN EXISTS (
                        SELECT 1
                        FROM StudyBoard sb
                        WHERE sb.id = :boardId AND sb.author.id = :memberId
                    ) THEN true ELSE false END
            """)
    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);
}
