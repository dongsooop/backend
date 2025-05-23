package com.dongsoop.dongsoop.study.repository;

import com.dongsoop.dongsoop.study.dto.StudyBoardDetails;
import com.dongsoop.dongsoop.study.dto.StudyBoardOverview;
import com.dongsoop.dongsoop.study.entity.QStudyBoard;
import com.dongsoop.dongsoop.study.entity.QStudyBoardApplication;
import com.dongsoop.dongsoop.study.entity.QStudyBoardDepartment;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyBoardRepositoryCustomImpl implements StudyBoardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public List<StudyBoardOverview> findStudyBoardOverviewsByPage(Pageable pageable) {
        QStudyBoard studyBoard = QStudyBoard.studyBoard;
        QStudyBoardDepartment studyBoardDepartment = QStudyBoardDepartment.studyBoardDepartment;

        return queryFactory
                .select(Projections.constructor(StudyBoardOverview.class,
                        studyBoard.id,
                        studyBoard.title,
                        studyBoard.content,
                        studyBoard.tags,
                        studyBoard.startAt,
                        studyBoard.endAt,
                        studyBoard.createdAt))
                .from(studyBoard)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .leftJoin(studyBoardDepartment)
                .fetch();
    }

    public Optional<StudyBoardDetails> findStudyBoardDetails(Long studyBoardId) {
        QStudyBoard studyBoard = QStudyBoard.studyBoard;
        QStudyBoardDepartment studyBoardDepartment = QStudyBoardDepartment.studyBoardDepartment;
        QStudyBoardApplication studyBoardApplication = QStudyBoardApplication.studyBoardApplication;

        StudyBoardDetails studyBoardDetails = queryFactory
                .select(Projections.constructor(StudyBoardDetails.class,
                        studyBoard.id,
                        studyBoard.title,
                        studyBoard.content,
                        studyBoard.tags,
                        studyBoard.startAt,
                        studyBoard.endAt,
                        Expressions.stringTemplate("listagg({0}, ',')", studyBoardDepartment.id.department.id),
                        studyBoard.author.nickname,
                        studyBoard.createdAt,
                        studyBoard.updatedAt,
                        studyBoardApplication.id.member.count().intValue()))
                .from(studyBoard)
                .leftJoin(studyBoardApplication)
                .on(studyBoardApplication.id.studyBoard.id.eq(studyBoard.id))
                .leftJoin(studyBoardDepartment)
                .on(studyBoardDepartment.id.studyBoard.id.eq(studyBoard.id))
                .groupBy(
                        studyBoard.id,
                        studyBoard.title,
                        studyBoard.content,
                        studyBoard.tags,
                        studyBoard.startAt,
                        studyBoard.endAt,
                        studyBoard.author.nickname,
                        studyBoard.createdAt,
                        studyBoard.updatedAt)
                .where(studyBoard.id.eq(studyBoardId))
                .fetchOne();

        return Optional.ofNullable(studyBoardDetails);
    }
}
