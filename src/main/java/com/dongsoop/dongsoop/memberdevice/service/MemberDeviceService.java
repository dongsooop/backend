package com.dongsoop.dongsoop.memberdevice.service;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import java.util.List;
import java.util.Map;

public interface MemberDeviceService {

    void registerDevice(String deviceToken, MemberDeviceType deviceType);

    void bindDeviceWithMemberId(Long memberId, String deviceToken);

    List<String> getDeviceByMemberId(Long memberId);

    Map<Long, List<String>> getDeviceByMember(MemberDeviceFindCondition condition);

    void deleteByToken(String deviceToken);
}
