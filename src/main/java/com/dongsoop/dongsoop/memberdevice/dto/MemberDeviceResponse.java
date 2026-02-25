package com.dongsoop.dongsoop.memberdevice.dto;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;

/**
 * 회원 기기 조회 응답 DTO.
 *
 * @param id      기기의 고유 ID
 * @param type    기기 타입 ({@link MemberDeviceType})
 * @param current 현재 요청을 보낸 기기 여부
 */
public record MemberDeviceResponse(
        Long id,
        MemberDeviceType type,
        boolean current
) {
}
