package com.dongsoop.dongsoop.memberdevice.service;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;

public interface MemberDeviceService {

    void registerDevice(String deviceToken, MemberDeviceType deviceType);

    void bindDeviceWithMemberId(Long memberId, String deviceToken);
}
