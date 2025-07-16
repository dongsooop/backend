package com.dongsoop.dongsoop.recruitment.apply.projection;

import com.dongsoop.dongsoop.department.entity.QDepartment;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.project.entity.QProjectApply;
import com.dongsoop.dongsoop.recruitment.board.project.entity.QProjectBoard;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import org.springframework.stereotype.Component;

@Component
public class ProjectRecruitmentApplyProjection implements RecruitmentApplyProjection {

    private static final QProjectBoard board = QProjectBoard.projectBoard;
    private static final QProjectApply apply = QProjectApply.projectApply;
    private static final QDepartment department = QDepartment.department;
    private static final QMember member = QMember.member;

    @Override
    public ConstructorExpression<ApplyDetails> getApplyDetailsExpression() {
        return Projections.constructor(ApplyDetails.class,
                board.id,
                member.id,
                member.nickname,
                department.name,
                apply.applyTime,
                apply.introduction,
                apply.motivation,
                apply.status);
    }
}
