package com.dongsoop.dongsoop.tutoring.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardDetails;
import com.dongsoop.dongsoop.tutoring.dto.TutoringBoardOverview;
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

    public List<TutoringBoardOverview> findTutoringBoardOverviewsByPage(Department recruitmentDepartment, Pageable pageable) {
        QTutoringBoard tutoringBoard = QTutoringBoard.tutoringBoard;

        return queryFactory.select(Projections.constructor(TutoringBoardOverview.class,
                        tutoringBoard.capacity,
                        tutoringBoard.endAt,
                        tutoringBoard.title,
                        tutoringBoard.content,
                        tutoringBoard.tags,
                        tutoringBoard.boardDate.createdAt))
                .from(tutoringBoard)
                .where(tutoringBoard.department.eq(recruitmentDepartment))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort()))
                .fetch();
    }

    public Optional<TutoringBoardDetails> findInformationById(Long tutoringBoardId) {
        QTutoringBoard tutoringBoard = QTutoringBoard.tutoringBoard;

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
                                tutoringBoard.boardDate.createdAt.as("createdAt"),
                                tutoringBoard.boardDate.updatedAt.as("updatedAt")
                        ))
                        .from(tutoringBoard)
                        .where(tutoringBoard.id.eq(tutoringBoardId))
                        .fetchOne());
    }
}
