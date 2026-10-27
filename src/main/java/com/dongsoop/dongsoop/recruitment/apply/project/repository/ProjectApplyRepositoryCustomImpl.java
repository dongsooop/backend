package com.dongsoop.dongsoop.recruitment.apply.project.repository;

import com.dongsoop.dongsoop.department.entity.QDepartment;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.project.entity.ProjectApply;
import com.dongsoop.dongsoop.recruitment.apply.project.entity.QProjectApply;
import com.dongsoop.dongsoop.recruitment.apply.projection.ProjectRecruitmentApplyProjection;
import com.dongsoop.dongsoop.recruitment.board.project.entity.QProjectBoard;
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

    private final ProjectRecruitmentApplyProjection projectRecruitmentApplyProjection;

    @Override
    public boolean existsByBoardIdAndMemberId(Long boardId, Long memberId) {
        return queryFactory.selectOne()
                .from(projectApply)
                .where(projectApply.projectBoard.id.eq(boardId)
                        .and(projectApply.member.id.eq(memberId)))
                .fetchFirst() != null;
    }

    @Override
    public Optional<ApplyDetails> findApplyDetailsByBoardIdAndApplierId(Long boardId, Long applierId) {
        ApplyDetails result = queryFactory.select(projectRecruitmentApplyProjection.getApplyDetailsExpression())
                .from(projectApply)
                .leftJoin(projectApply.projectBoard, projectBoard)
                .leftJoin(projectApply.member, member)
                .leftJoin(member.department, department)
                .where(projectApply.projectBoard.id.eq(boardId)
                        .and(projectApply.member.id.eq(applierId)))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<ProjectApply> findByBoardIdAndApplierId(Long boardId, Long applierId) {
        ProjectApply result = queryFactory.selectFrom(projectApply)
                .where(projectApply.projectBoard.id.eq(boardId)
                        .and(projectApply.member.id.eq(applierId)))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
