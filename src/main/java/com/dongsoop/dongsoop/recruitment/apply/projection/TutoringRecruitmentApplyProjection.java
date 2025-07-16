package com.dongsoop.dongsoop.recruitment.apply.projection;

import com.dongsoop.dongsoop.department.entity.QDepartment;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.entity.QTutoringApply;
import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.QTutoringBoard;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import org.springframework.stereotype.Component;

@Component
public class TutoringRecruitmentApplyProjection implements RecruitmentApplyProjection {

    private static final QTutoringBoard board = QTutoringBoard.tutoringBoard;
    private static final QTutoringApply apply = QTutoringApply.tutoringApply;
    private static final QMember member = QMember.member;
    private static final QDepartment department = QDepartment.department;

    @Override
    public ConstructorExpression<ApplyDetails> getApplyDetailsExpression() {
        return Projections.constructor(ApplyDetails.class,
                board.id,
                board.title,
                member.id,
                member.nickname,
                department.name,
                apply.applyTime,
                apply.introduction,
                apply.motivation,
                apply.status);
    }
}
