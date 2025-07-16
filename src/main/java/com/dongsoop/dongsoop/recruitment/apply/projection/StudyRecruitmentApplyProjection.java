package com.dongsoop.dongsoop.recruitment.apply.projection;

import com.dongsoop.dongsoop.department.entity.QDepartment;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.study.entity.QStudyApply;
import com.dongsoop.dongsoop.recruitment.board.study.entity.QStudyBoard;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import org.springframework.stereotype.Component;

@Component
public class StudyRecruitmentApplyProjection implements RecruitmentApplyProjection {

    private static final QStudyBoard board = QStudyBoard.studyBoard;
    private static final QStudyApply apply = QStudyApply.studyApply;
    private static final QMember member = QMember.member;
    private static final QDepartment department = QDepartment.department;

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
