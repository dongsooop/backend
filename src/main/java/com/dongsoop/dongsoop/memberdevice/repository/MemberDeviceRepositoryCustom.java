package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.memberdevice.dto.DeviceSubscription;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceResponse;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MemberDeviceRepositoryCustom {

    List<MemberDeviceDto> findDevicesWithNotification(MemberDeviceFindCondition condition);

    List<String> getDeviceByMemberId(Long memberId);

    List<MemberDeviceResponse> findDeviceListByMemberId(Long memberId, String currentDeviceToken);

    long deleteExpiredDevices(LocalDateTime cutoff);

    /**
     * 여러 학과의 공지 발송 대상 기기를 한 번에 조회한다.
     *
     * <p>대학 공지(전체 학과)가 섞여 있으면 구독 여부와 무관하게 전체 기기가 대상이므로
     * 구독 조건을 걸지 않는다. 학과별로 나누는 일은 {@link #findSubscriptionsByDeviceIds} 결과로 처리한다.
     */
    List<MemberDevice> searchDevicesByDepartments(Collection<DepartmentType> departmentTypes);

    /**
     * 주어진 기기들이 구독한 (기기 id, 학과) 쌍을 모두 반환한다.
     */
    List<DeviceSubscription> findSubscriptionsByDeviceIds(Collection<Long> deviceIds);
}
