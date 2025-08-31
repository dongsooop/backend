package com.dongsoop.dongsoop.memberdevice.service;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MemberDeviceService {

    void registerDevice(String deviceToken, MemberDeviceType deviceType);

    void bindDeviceWithMemberId(Long memberId, String deviceToken);

    Map<Long, List<String>> getDeviceByMember(Collection<Long> memberIdList);

    void deleteByToken(String deviceToken);
}
