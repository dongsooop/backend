package com.dongsoop.dongsoop.memberdevice.repository;

import static com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice.memberDevice;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceResponse;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.micrometer.common.util.StringUtils;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberDeviceRepositoryCustomImpl implements MemberDeviceRepositoryCustom {

    private final JPAQueryFactory queryFactory;


    @Override
    public List<String> getDeviceByMemberId(Long memberId) {
        return queryFactory.select(memberDevice.deviceToken)
                .from(memberDevice)
                .where(memberDevice.member.id.eq(memberId)
                        .and(isNotWebDevice())
                        .and(memberDevice.deviceToken.isNotNull()))
                .fetch();
    }

    @Override
    public List<MemberDeviceResponse> findDeviceListByMemberId(Long memberId, String currentDeviceToken) {
        Expression<Boolean> isCurrentDevice = isCurrentDevice(currentDeviceToken);

        return queryFactory.select(Projections.constructor(MemberDeviceResponse.class,
                        memberDevice.id,
                        memberDevice.memberDeviceType,
                        isCurrentDevice,
                        memberDevice.updatedAt))
                .from(memberDevice)
                .where(memberDevice.member.id.eq(memberId))
                .fetch();
    }

    private Expression<Boolean> isCurrentDevice(String currentDeviceToken) {
        if (StringUtils.isBlank(currentDeviceToken)) {
            return Expressions.FALSE;
        }

        return memberDevice.deviceToken.eq(currentDeviceToken);
    }

    private BooleanExpression isNotWebDevice() {
        return memberDevice.memberDeviceType.ne(MemberDeviceType.WEB);
    }

    @Override
    public long deleteExpiredDevices(LocalDateTime cutoff) {
        return queryFactory.delete(memberDevice)
                .where(memberDevice.lastAccess.lt(cutoff)
                        .and(memberDevice.memberDeviceType.eq(MemberDeviceType.WEB)
                                .or(memberDevice.deviceToken.isNull())))
                .execute();
    }
}
