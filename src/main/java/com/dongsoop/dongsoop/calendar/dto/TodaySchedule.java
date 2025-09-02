package com.dongsoop.dongsoop.calendar.dto;

import com.dongsoop.dongsoop.member.entity.Member;

public record TodaySchedule(

        String title,
        Member member
) {
}
