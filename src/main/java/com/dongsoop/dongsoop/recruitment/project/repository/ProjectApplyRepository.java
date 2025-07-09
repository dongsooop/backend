package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.recruitment.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectApply;
import com.dongsoop.dongsoop.recruitment.project.entity.ProjectApply.ProjectApplyKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectApplyRepository extends JpaRepository<ProjectApply, ProjectApplyKey> {

    @Query(
            """
                    SELECT m.id AS memberId,
                        m.nickname AS memberName,
                        pa.status AS status,
                        m.department.name AS departmentName
                    FROM ProjectApply pa
                    JOIN Member m ON pa.id.member.id = m.id
                    WHERE pa.id.projectBoard.id = :boardId
                        AND pa.id.projectBoard.author.id = :authorId
                    """
    )
    List<RecruitmentApplyOverview> findApplyOverviewByBoardId(@Param("boardId") Long boardId,
                                                              @Param("authorId") Long authorId);
}
