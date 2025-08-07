package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberDeviceRepositoryCustomImpl implements MemberDeviceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QMemberDevice memberDevice = QMemberDevice.memberDevice;
    private final QMember member = QMember.member;

    public List<String> getMemberDeviceByDepartment(Department department) {
        return queryFactory.select(memberDevice.deviceToken)
                .from(memberDevice)
                .leftJoin(memberDevice.member, member)
                .where(member.department.eq(department))
                .fetch();
    }
}
