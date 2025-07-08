package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.recruitment.project.entity.ProjectBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProjectBoardRepository extends JpaRepository<ProjectBoard, Long> {

    boolean existsByIdAndAuthorId(Long id, Long authorId);

    @Query("""
                    SELECT CASE WHEN EXISTS (
                        SELECT 1
                        FROM ProjectBoard pb
                        WHERE pb.id = :boardId AND pb.author.id = :memberId
                    ) THEN true ELSE false END
            """)
    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);
}
