package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceResponse;
import java.time.LocalDateTime;
import java.util.List;

public interface MemberDeviceRepositoryCustom {


    List<String> getDeviceByMemberId(Long memberId);

    List<MemberDeviceResponse> findDeviceListByMemberId(Long memberId, String currentDeviceToken);

    long deleteExpiredDevices(LocalDateTime cutoff);

}
