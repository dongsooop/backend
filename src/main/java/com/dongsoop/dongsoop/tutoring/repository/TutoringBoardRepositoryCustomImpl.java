package com.dongsoop.dongsoop.tutoring.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardDetails;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardOverview;
import com.dongsoop.dongsoop.tutoring.entity.QTutoringApplication;
import com.dongsoop.dongsoop.tutoring.entity.QTutoringBoard;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TutoringBoardRepositoryCustomImpl implements TutoringBoardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final PageableUtil pageableUtil;

    public List<TutoringBoardOverview> findTutoringBoardOverviewsByPage(Department recruitmentDepartment,
                                                                        Pageable pageable) {
        QTutoringBoard tutoringBoard = QTutoringBoard.tutoringBoard;
        QTutoringApplication tutoringApplication = QTutoringApplication.tutoringApplication;

        return queryFactory.select(Projections.constructor(TutoringBoardOverview.class,
                        tutoringBoard.id,
                        tutoringBoard.capacity,
                        tutoringApplication.id.member.count().intValue(),
                        tutoringBoard.startAt,
                        tutoringBoard.endAt,
                        tutoringBoard.title,
                        tutoringBoard.content,
                        tutoringBoard.tags))
                .from(tutoringBoard)
                .leftJoin(tutoringApplication)
                .on(tutoringApplication.id.tutoringBoard.id.eq(tutoringBoard.id))
                .where(tutoringBoard.department.eq(recruitmentDepartment))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(tutoringBoard.id)
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort()))
                .fetch();
    }

    public Optional<TutoringBoardDetails> findInformationById(Long tutoringBoardId) {
        QTutoringBoard tutoringBoard = QTutoringBoard.tutoringBoard;
        QTutoringApplication tutoringApplication = QTutoringApplication.tutoringApplication;

        return Optional.ofNullable(
                queryFactory.select(Projections.constructor(TutoringBoardDetails.class,
                                tutoringBoard.id,
                                tutoringBoard.title,
                                tutoringBoard.content,
                                tutoringBoard.tags,
                                tutoringBoard.capacity,
                                tutoringBoard.startAt,
                                tutoringBoard.endAt,
                                tutoringBoard.department.id,
                                tutoringBoard.author.nickname,
                                tutoringBoard.createdAt.as("createdAt"),
                                tutoringBoard.updatedAt.as("updatedAt"),
                                tutoringApplication.id.member.count().intValue()
                        ))
                        .from(tutoringBoard)
                        .leftJoin(tutoringApplication)
                        .on(tutoringApplication.id.tutoringBoard.id.eq(tutoringBoard.id))
                        .where(tutoringBoard.id.eq(tutoringBoardId))
                        .groupBy(tutoringBoard.id, tutoringBoard.author.nickname)
                        .fetchOne());
    }
}
