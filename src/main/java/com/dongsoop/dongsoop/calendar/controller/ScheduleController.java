package com.dongsoop.dongsoop.calendar.controller;

import com.dongsoop.dongsoop.calendar.dto.CreateMemberScheduleRequest;
import com.dongsoop.dongsoop.calendar.dto.ScheduleDetails;
import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import com.dongsoop.dongsoop.calendar.service.ScheduleService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/member/{memberId}/month/{yearMonth}")
    public ResponseEntity<List<ScheduleDetails>> getMemberSchedule(@PathVariable("memberId") Long memberId,
                                                                   @PathVariable("yearMonth") YearMonth yearMonth) {
        List<ScheduleDetails> scheduleList = scheduleService.getMemberSchedule(memberId, yearMonth);
        return ResponseEntity.ok(scheduleList);
    }

    @PostMapping("/member")
    public ResponseEntity<Void> createMemberSchedule(@RequestBody @Valid CreateMemberScheduleRequest request) {
        MemberSchedule schedule = scheduleService.createMemberSchedule(request);
        URI uri = URI.create("/schedule/member/" + schedule.getId().toString());

        return ResponseEntity.created(uri)
                .build();
    }
}
