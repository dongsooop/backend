package com.dongsoop.dongsoop.timetable.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.timetable.dto.CreateTimetableRequest;
import com.dongsoop.dongsoop.timetable.entity.Timetable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimetableMapper {

    private final MemberService memberService;

    public Timetable toEntity(CreateTimetableRequest request) {
        Member referenceMember = memberService.getMemberReferenceByContext();

        return Timetable.builder()
                .name(request.name())
                .professor(request.professor())
                .location(request.location())
                .week(request.week())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .year(request.year())
                .semester(request.semester())
                .member(referenceMember)
                .build();
    }
}
