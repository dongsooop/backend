package com.dongsoop.dongsoop.calendar.dto;

import com.dongsoop.dongsoop.member.entity.Member;

public record TodaySchedule(

        Long id,
        String title,
        Member member
) {
}
