package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.recruitment.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyApply;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyApply.StudyApplyKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyApplyRepository extends JpaRepository<StudyApply, StudyApplyKey> {

    @Query(
            """
                    SELECT m.id AS memberId,
                        m.nickname AS memberName,
                        sa.status AS status,
                        m.department.name AS departmentName
                    FROM StudyApply sa
                    JOIN Member m ON sa.id.member.id = m.id
                    WHERE sa.id.studyBoard.id = :boardId
                        AND sa.id.studyBoard.author.id = :authorId
                    """
    )
    List<RecruitmentApplyOverview> findApplyOverviewByBoardId(@Param("boardId") Long boardId,
                                                              @Param("authorId") Long authorId);
}
