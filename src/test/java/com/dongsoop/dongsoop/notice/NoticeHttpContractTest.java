package com.dongsoop.dongsoop.notice;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.notice.controller.NoticeController;
import com.dongsoop.dongsoop.notice.service.NoticeSchedulerImpl;
import com.dongsoop.dongsoop.notice.service.NoticeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NoticeHttpContractTest {
  @Mock NoticeService service;
  @Mock NoticeSchedulerImpl scheduler;
  @InjectMocks NoticeController controller;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
  }

  @Test
  void departmentPathAndPagingAreBound() throws Exception {
    var page = PageRequest.of(2, 5);
    when(service.getNoticeByDepartmentType(DepartmentType.DEPT_2001, page))
        .thenReturn(Page.empty(page));
    mvc.perform(get("/notice/DEPT_2001").param("page", "2").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(2))
        .andExpect(jsonPath("$.size").value(5));
    verify(service).getNoticeByDepartmentType(DepartmentType.DEPT_2001, page);
  }

  @Test
  void guestHeadersArePassedTogetherAndRemainOptional() throws Exception {
    var page = PageRequest.of(0, 20);
    when(service.getSubscribedNotices("fid", "token", page)).thenReturn(Page.empty(page));
    when(service.getSubscribedNotices(null, null, page)).thenReturn(Page.empty(page));
    mvc.perform(
            get("/notice/guest").header("X-Device-Fid", "fid").header("X-Device-Token", "token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());
    mvc.perform(get("/notice/guest")).andExpect(status().isOk());
    verify(service).getSubscribedNotices("fid", "token", page);
    verify(service).getSubscribedNotices(null, null, page);
  }

  @Test
  void unknownDepartmentCannotReachService() throws Exception {
    mvc.perform(get("/notice/UNKNOWN")).andExpect(status().isBadRequest());
    verifyNoInteractions(service, scheduler);
  }
}
