package com.dongsoop.dongsoop.notice.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.entity.QDepartment;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.notice.dto.HomeNotice;
import com.dongsoop.dongsoop.notice.dto.NoticeType;
import com.dongsoop.dongsoop.notice.entity.QNotice;
import com.dongsoop.dongsoop.notice.entity.QNoticeDetails;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NoticeRepositoryCustomImpl implements NoticeRepositoryCustom {

    private static final QNotice notice = QNotice.notice;
    private static final QNoticeDetails noticeDetails = QNoticeDetails.noticeDetails;
    private static final QMember member = QMember.member;
    private static final QDepartment department = QDepartment.department;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<HomeNotice> searchHomeNotices(DepartmentType departmentType) {
        return queryFactory.select(Projections.constructor(
                        HomeNotice.class,
                        noticeDetails.title,
                        noticeDetails.link,
                        getNoticeType(department)))
                .from(notice)
                .innerJoin(notice.id.noticeDetails, noticeDetails)
                .innerJoin(notice.id.department, department)
                .where(notice.id.department.id.in(List.of(DepartmentType.DEPT_1001, departmentType))) // 사용자 학과 및 대학 공지
                .orderBy(noticeDetails.createdAt.desc())
                .limit(3)
                .fetch();
    }

    private Expression<NoticeType> getNoticeType(QDepartment department) {
        return new CaseBuilder()
                .when(department.id.eq(DepartmentType.DEPT_1001))
                .then(Expressions.constant(NoticeType.OFFICIAL))
                .otherwise(Expressions.constant(NoticeType.DEPARTMENT));
    }
}
