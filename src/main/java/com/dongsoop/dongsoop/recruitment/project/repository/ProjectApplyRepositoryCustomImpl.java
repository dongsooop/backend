package com.dongsoop.dongsoop.recruitment.project.repository;

import com.dongsoop.dongsoop.department.entity.QDepartment;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.recruitment.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectApply;
import com.dongsoop.dongsoop.recruitment.project.entity.QProjectBoard;
import com.dongsoop.dongsoop.recruitment.projection.ProjectRecruitmentProjection;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectApplyRepositoryCustomImpl implements ProjectApplyRepositoryCustom {

    private static final QProjectApply projectApply = QProjectApply.projectApply;
    private static final QProjectBoard projectBoard = QProjectBoard.projectBoard;
    private static final QMember member = QMember.member;
    private static final QDepartment department = QDepartment.department;

    private final JPAQueryFactory queryFactory;

    private final ProjectRecruitmentProjection projectRecruitmentProjection;

    @Override
    public boolean existsByBoardIdAndMemberId(Long boardId, Long memberId) {
        return queryFactory.selectOne()
                .from(projectApply)
                .where(projectApply.id.projectBoard.id.eq(boardId)
                        .and(projectApply.id.member.id.eq(memberId)))
                .fetchFirst() != null;
    }

    @Override
    public void updateApplyStatus(Long memberId, Long boardId, RecruitmentApplyStatus status) {
        queryFactory.update(projectApply)
                .where(projectApply.id.projectBoard.id.eq(boardId)
                        .and(projectApply.id.member.id.eq(memberId)))
                .set(projectApply.status, status)
                .execute();
    }

    @Override
    public Optional<ApplyDetails> findApplyDetailsByBoardIdAndApplierId(Long boardId, Long applierId) {
        ApplyDetails result = queryFactory.select(projectRecruitmentProjection.getApplyDetailsExpression())
                .from(projectApply)
                .leftJoin(projectApply.id.projectBoard, projectBoard)
                .leftJoin(projectApply.id.member, member)
                .leftJoin(member.department, department)
                .where(projectApply.id.projectBoard.id.eq(boardId)
                        .and(projectApply.id.member.id.eq(applierId)))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
