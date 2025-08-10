package com.dongsoop.dongsoop.memberdevice.service;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;

public interface MemberDeviceService {

    void registerDeviceByMemberId(Long memberId, String deviceToken, MemberDeviceType deviceType);
}
