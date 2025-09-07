package com.dongsoop.dongsoop.home.dto;

import com.dongsoop.dongsoop.calendar.dto.HomeSchedule;
import com.dongsoop.dongsoop.notice.dto.HomeNotice;
import com.dongsoop.dongsoop.recruitment.board.dto.HomeRecruitment;
import com.dongsoop.dongsoop.timetable.dto.HomeTimetable;
import java.util.List;

public record HomeDto(

        List<HomeTimetable> timetable,
        List<HomeSchedule> schedules,
        List<HomeNotice> notices,
        List<HomeRecruitment> popular_recruitments
) {
}
