package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberDeviceRepositoryCustomImpl implements MemberDeviceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QMemberDevice memberDevice = QMemberDevice.memberDevice;

    @Override
    public List<MemberDeviceDto> getAllMemberDevice() {
        return queryFactory.select(Projections.constructor(MemberDeviceDto.class,
                        memberDevice.member, memberDevice.deviceToken))
                .from(memberDevice)
                .fetch();
    }

    @Override
    public List<MemberDeviceDto> getMemberDeviceByDepartment(Department department) {
        return queryFactory.select(Projections.constructor(MemberDeviceDto.class,
                        memberDevice.member, memberDevice.deviceToken))
                .from(memberDevice)
                .where(memberDevice.member.department.eq(department))
                .fetch();
    }

    @Override
    public List<MemberDeviceDto> getMemberDeviceTokenByMemberIds(Set<Long> memberIds) {
        return queryFactory.select(Projections.constructor(MemberDeviceDto.class,
                        memberDevice.member, memberDevice.deviceToken))
                .from(memberDevice)
                .where(memberDevice.member.id.in(memberIds))
                .fetch();
    }

    @Override
    public List<String> getMemberDeviceTokenByMemberId(Long memberId) {
        return queryFactory.select(memberDevice.deviceToken)
                .from(memberDevice)
                .where(memberDevice.member.id.eq(memberId))
                .fetch();
    }
}
