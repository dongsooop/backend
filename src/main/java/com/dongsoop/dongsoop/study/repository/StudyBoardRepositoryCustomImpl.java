package com.dongsoop.dongsoop.study.repository;

import com.dongsoop.dongsoop.study.dto.StudyBoardDetails;
import com.dongsoop.dongsoop.study.dto.StudyBoardOverview;
import com.dongsoop.dongsoop.study.entity.QStudyBoard;
import com.dongsoop.dongsoop.study.entity.QStudyBoardApplication;
import com.dongsoop.dongsoop.study.entity.QStudyBoardDepartment;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StudyBoardRepositoryCustomImpl implements StudyBoardRepositoryCustom {

    private static final QStudyBoard studyBoard = QStudyBoard.studyBoard;

    private static final QStudyBoardDepartment studyBoardDepartment = QStudyBoardDepartment.studyBoardDepartment;

    private static final QStudyBoardApplication studyBoardApplication = QStudyBoardApplication.studyBoardApplication;

    private final JPAQueryFactory queryFactory;

    public List<StudyBoardOverview> findStudyBoardOverviewsByPage(Pageable pageable) {
        return queryFactory
                .select(Projections.constructor(StudyBoardOverview.class,
                        studyBoard.id,
                        studyBoardApplication.id.member.countDistinct().intValue(),
                        studyBoard.startAt,
                        studyBoard.endAt,
                        studyBoard.title,
                        studyBoard.content,
                        studyBoard.tags))
                .from(studyBoard)
                .leftJoin(studyBoardApplication)
                .on(equalStudyBoardId(studyBoardApplication.id.studyBoard.id))
                .leftJoin(studyBoardDepartment)
                .on(equalStudyBoardId(studyBoardDepartment.id.studyBoard.id))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .groupBy(studyBoard.id)
                .fetch();
    }

    public Optional<StudyBoardDetails> findStudyBoardDetails(Long studyBoardId) {
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
                .on(equalStudyBoardId(studyBoardApplication.id.studyBoard.id))
                .leftJoin(studyBoardDepartment)
                .on(equalStudyBoardId(studyBoardDepartment.id.studyBoard.id))
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

    private BooleanExpression equalStudyBoardId(NumberPath<Long> studyBoardId) {
        return studyBoard.id.eq(studyBoardId);
    }
}
