package com.dongsoop.dongsoop.memberdevice.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;

/**
 * 기기 하나가 구독한 학과 한 건.
 *
 * <p>여러 학과의 공지를 한 번에 처리할 때, 기기 조회와 별개로 "어느 기기가 어느 학과를 구독했는지"를
 * 받아 메모리에서 학과별로 나누는 데 쓴다.
 */
public record DeviceSubscription(
        Long deviceId,
        DepartmentType departmentType
) {
}
