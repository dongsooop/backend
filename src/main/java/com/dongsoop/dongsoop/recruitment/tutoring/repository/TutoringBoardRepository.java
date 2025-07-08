package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TutoringBoardRepository extends JpaRepository<TutoringBoard, Long> {

    boolean existsByIdAndAuthorId(Long id, Long authorId);

    @Query("""
                    SELECT CASE WHEN EXISTS (
                        SELECT 1
                        FROM TutoringBoard tb
                        WHERE tb.id = :boardId AND tb.author.id = :memberId
                    ) THEN true ELSE false END
            """)
    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);
}
