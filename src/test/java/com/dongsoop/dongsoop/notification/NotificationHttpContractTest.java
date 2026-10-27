package com.dongsoop.dongsoop.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.notification.controller.NotificationController;
import com.dongsoop.dongsoop.notification.dto.NotificationOverview;
import com.dongsoop.dongsoop.notification.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NotificationHttpContractTest {
    @Mock NotificationService notifications;
    @Mock MemberService members;
    @InjectMocks NotificationController controller;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }

    @Test
    void listBindsPagingAndReturnsUnreadOverviewForServerMember() throws Exception {
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        when(notifications.getNotifications(any(Pageable.class), eq(7L)))
                .thenReturn(new NotificationOverview(List.of(), 12L));

        mvc.perform(get("/notifications").param("page", "2").param("size", "5")
                        .param("sort", "id,desc").param("memberId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notificationLists").isEmpty())
                .andExpect(jsonPath("$.unreadCount").value(12));

        var page = ArgumentCaptor.forClass(Pageable.class);
        verify(notifications).getNotifications(page.capture(), eq(7L));
        assertThat(page.getValue().getPageNumber()).isEqualTo(2);
        assertThat(page.getValue().getPageSize()).isEqualTo(5);
        assertThat(page.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Test
    void readIgnoresClientMemberAndUsesAuthenticatedMember() throws Exception {
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        mvc.perform(post("/notifications/read").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":19,\"memberId\":999}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(notifications).read(19L, 7L);
    }

    @Test
    void readAllAndDeleteUseAuthenticatedMember() throws Exception {
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        mvc.perform(post("/notifications/read-all")).andExpect(status().isNoContent());
        mvc.perform(delete("/notifications/19").param("memberId", "999")).andExpect(status().isNoContent());
        verify(notifications).readAll(7L);
        verify(notifications).deleteMemberNotification(19L, 7L);
    }

    @Test
    void malformedIdCannotReachService() throws Exception {
        mvc.perform(delete("/notifications/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(post("/notifications/read").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(notifications, members);
    }
}
