package com.dongsoop.dongsoop.recruitment.apply.tutoring.repository;

import com.dongsoop.dongsoop.recruitment.apply.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.entity.TutoringApply;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.entity.TutoringApply.TutoringApplyKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TutoringApplyRepository extends JpaRepository<TutoringApply, TutoringApplyKey> {

    @Query(
            """
                    SELECT m.id AS memberId,
                        m.nickname AS memberName,
                        ta.status AS status,
                        m.department.name AS departmentName
                    FROM TutoringApply ta
                    JOIN Member m ON ta.id.member.id = m.id
                    WHERE ta.id.tutoringBoard.id = :boardId
                        AND ta.id.tutoringBoard.author.id = :authorId
                    """
    )
    List<RecruitmentApplyOverview> findApplyOverviewByBoardId(@Param("boardId") Long boardId,
                                                              @Param("authorId") Long authorId);
}
