package com.dongsoop.dongsoop.calendar.controller;

import com.dongsoop.dongsoop.calendar.dto.CreateMemberScheduleRequest;
import com.dongsoop.dongsoop.calendar.dto.MemberScheduleUpdateRequest;
import com.dongsoop.dongsoop.calendar.dto.ScheduleDetails;
import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import com.dongsoop.dongsoop.calendar.service.ScheduleService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final MemberService memberService;

    @GetMapping("/{yearMonth}")
    public ResponseEntity<List<ScheduleDetails>> getSchedule(
            @PathVariable("yearMonth") @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        // 인증되지 않은 사용자일 경우
        if (!memberService.isAuthenticated()) {
            List<ScheduleDetails> scheduleList = scheduleService.getSchedule(yearMonth);

            return ResponseEntity.ok(scheduleList);
        }

        // 인증된 사용자일 경우
        Long memberId = memberService.getMemberIdByAuthentication();
        List<ScheduleDetails> scheduleList = scheduleService.getSchedule(memberId, yearMonth);
        return ResponseEntity.ok(scheduleList);
    }

    @PostMapping("/member")
    public ResponseEntity<Void> createMemberSchedule(@RequestBody @Valid CreateMemberScheduleRequest request) {
        Member memberReferenceByContext = memberService.getMemberReferenceByContext();
        MemberSchedule schedule = scheduleService.createMemberSchedule(memberReferenceByContext, request);

        Long memberId = schedule.getMember()
                .getId();
        YearMonth startAtYearMonth = YearMonth.from(request.getStartAt());

        URI uri = URI.create("/schedule/member/" + memberId + "/year-month/" + startAtYearMonth);

        return ResponseEntity.created(uri)
                .build();
    }

    @DeleteMapping("/member/{scheduleId}")
    public ResponseEntity<Void> deleteMemberSchedule(@PathVariable("scheduleId") Long scheduleId) {
        Long memberIdByAuthentication = memberService.getMemberIdByAuthentication();
        scheduleService.deleteMemberSchedule(scheduleId, memberIdByAuthentication);

        return ResponseEntity.noContent()
                .build();
    }

    @PatchMapping("/member/{scheduleId}")
    public ResponseEntity<Void> updateMemberSchedule(@PathVariable("scheduleId") Long scheduleId,
                                                     @RequestBody @Valid MemberScheduleUpdateRequest request) {
        Long memberId = memberService.getMemberIdByAuthentication();
        scheduleService.updateMemberSchedule(scheduleId, memberId, request);
        return ResponseEntity.noContent()
                .build();
    }
}
