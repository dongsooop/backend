package com.dongsoop.dongsoop.memberdevice.dto;

import com.dongsoop.dongsoop.member.entity.Member;

public record MemberDeviceDto(

        Member member,
        String deviceToken
) {
}
