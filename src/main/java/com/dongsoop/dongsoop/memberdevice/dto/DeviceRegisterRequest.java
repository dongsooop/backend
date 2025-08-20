package com.dongsoop.dongsoop.memberdevice.dto;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;

public record DeviceRegisterRequest(

        String deviceToken,
        MemberDeviceType type
) {
}
