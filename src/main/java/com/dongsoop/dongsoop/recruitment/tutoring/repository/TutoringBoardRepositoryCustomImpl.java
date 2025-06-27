package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.mypage.dto.OpenedRecruitment;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.projection.TutoringRecruitmentProjection;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringApply;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.QTutoringBoard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TutoringBoardRepositoryCustomImpl implements TutoringBoardRepositoryCustom {

    private static final QTutoringBoard tutoringBoard = QTutoringBoard.tutoringBoard;

    private static final QTutoringApply tutoringApply = QTutoringApply.tutoringApply;

    private final JPAQueryFactory queryFactory;

    private final PageableUtil pageableUtil;

    private final TutoringRecruitmentProjection projection;

    @Override
    public List<RecruitmentOverview> findTutoringBoardOverviewsByPageAndDepartmentType(DepartmentType departmentType,
                                                                                       Pageable pageable) {
        return queryFactory.select(projection.getRecruitmentOverviewExpression())
                .from(tutoringBoard)
                .leftJoin(tutoringApply)
                .on(tutoringApply.id.tutoringBoard.id.eq(tutoringBoard.id))
                .where(tutoringBoard.department.id.eq(departmentType))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(tutoringBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), tutoringBoard))
                .fetch();
    }

    @Override
    public Optional<RecruitmentDetails> findBoardDetailsByIdAndViewType(Long tutoringBoardId,
                                                                        RecruitmentViewType viewType,
                                                                        boolean isAlreadyApplied) {
        RecruitmentDetails details = queryFactory.select(
                        projection.getRecruitmentDetailsExpression(viewType, isAlreadyApplied))
                .from(tutoringBoard)
                .leftJoin(tutoringApply)
                .on(tutoringApply.id.tutoringBoard.id.eq(tutoringBoard.id))
                .where(tutoringBoard.id.eq(tutoringBoardId))
                .groupBy(tutoringBoard.id, tutoringBoard.author.nickname)
                .fetchOne();

        return Optional.ofNullable(details);
    }

    @Override
    public List<RecruitmentOverview> findTutoringBoardOverviewsByPage(Pageable pageable) {
        return queryFactory.select(projection.getRecruitmentOverviewExpression())
                .from(tutoringBoard)
                .leftJoin(tutoringApply)
                .on(tutoringApply.id.tutoringBoard.id.eq(tutoringBoard.id))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(tutoringBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), tutoringBoard))
                .fetch();
    }

    @Override
    public List<OpenedRecruitment> findOpenedRecruitmentsByMemberId(Long memberId, Pageable pageable) {
        return queryFactory
                .select(projection.getOpenedRecruitmentExpression())
                .from(tutoringBoard)
                .leftJoin(tutoringApply)
                .on(tutoringApply.id.tutoringBoard.id.eq(tutoringBoard.id)
                        .and(tutoringApply.id.member.id.eq(memberId)))
                .where(tutoringApply.id.member.id.eq(memberId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(tutoringBoard.id)
                .orderBy(tutoringBoard.createdAt.desc())
                .fetch();
    }
}
