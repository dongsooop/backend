package com.dongsoop.dongsoop.calendar;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dongsoop.dongsoop.calendar.controller.ScheduleController;
import com.dongsoop.dongsoop.calendar.dto.CreateMemberScheduleRequest;
import com.dongsoop.dongsoop.calendar.dto.MemberScheduleUpdateRequest;
import com.dongsoop.dongsoop.calendar.dto.ScheduleDetails;
import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import com.dongsoop.dongsoop.calendar.service.ScheduleService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ScheduleHttpContractTest {
    @Mock ScheduleService schedules;
    @Mock MemberService members;
    @InjectMocks ScheduleController controller;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void guestMonthReturnsPublicSchedulesWithoutMemberLookup() throws Exception {
        when(schedules.getSchedule(YearMonth.of(2026, 10)))
                .thenReturn(List.of(ScheduleDetails.builder().id(11L).title("Public event").build()));
        mvc.perform(get("/schedule/2026-10")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11)).andExpect(jsonPath("$[0].title").value("Public event"));
        verify(members, never()).getMemberIdByAuthentication();
    }

    @Test
    void memberMonthUsesServerIdentity() throws Exception {
        when(members.isAuthenticated()).thenReturn(true);
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        when(schedules.getSchedule(7L, YearMonth.of(2026, 10))).thenReturn(List.of());
        mvc.perform(get("/schedule/2026-10").param("memberId", "999"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(schedules, never()).getSchedule(any(YearMonth.class));
    }

    @Test
    void createBindsDatesAndReturnsMemberMonthLocation() throws Exception {
        var member = Member.builder().id(7L).build();
        when(members.getMemberReferenceByContext()).thenReturn(member);
        when(schedules.createMemberSchedule(eq(member), any(CreateMemberScheduleRequest.class)))
                .thenReturn(MemberSchedule.builder().member(member).build());
        mvc.perform(post("/schedule/member").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Meeting","location":"Room101","startAt":"2026-10-31T23:00:00",
                 "endAt":"2026-11-01T01:00:00","color":"abcdef","memberId":999}
                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/schedule/member/7/year-month/2026-10"));
        verify(schedules).createMemberSchedule(eq(member), argThat(r -> r.getTitle().equals("Meeting")
                && r.getStartAt().equals(LocalDateTime.of(2026, 10, 31, 23, 0))
                && r.getEndAt().equals(LocalDateTime.of(2026, 11, 1, 1, 0))));
    }

    @Test
    void patchAndDeleteUseServerIdentity() throws Exception {
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        mvc.perform(patch("/schedule/member/19").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\",\"memberId\":999}"))
                .andExpect(status().isNoContent());
        verify(schedules).updateMemberSchedule(eq(19L), eq(7L), any(MemberScheduleUpdateRequest.class));
        mvc.perform(delete("/schedule/member/19").param("memberId", "999")).andExpect(status().isNoContent());
        verify(schedules).deleteMemberSchedule(19L, 7L);
    }

    @Test
    void invalidMonthAndRequestCannotReachServices() throws Exception {
        mvc.perform(get("/schedule/2026-13")).andExpect(status().isBadRequest());
        mvc.perform(post("/schedule/member").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/schedule/member/19").contentType(MediaType.APPLICATION_JSON).content("{\"color\":\"#ffffff\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(schedules, members);
    }
}
